package dev.ua.theroer.doublelife.doublelife.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.theroer.doublelife.config.WebhookSettings;
import dev.ua.theroer.magicutils.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebhookManager {

    private final WebhookSettings settings;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final ScheduledExecutorService aggregator = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "doublelife-webhook-aggregator");
        t.setDaemon(true);
        return t;
    });
    private final Map<UUID, List<String>> pendingActions = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingNames = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastActionMessageId = new ConcurrentHashMap<>();

    public WebhookManager(WebhookSettings settings) {
        this.settings = settings;
    }

    public void sendStartNotification(String playerName, UUID playerUuid, String profiles, String duration) {
        if (!settings.isEnabled() || settings.getUrl() == null || settings.getUrl().isEmpty()) {
            return;
        }

        String message = format(settings.getStartMessage())
            .replace("{player}", playerName)
            .replace("{profile}", profiles)
            .replace("{duration}", duration)
            .replace("{time}", TIME_FORMAT.format(Instant.now()));

        sendWebhook(message, 0x00FF00, playerUuid);
    }

    public void sendEndNotification(String playerName, UUID playerUuid, String profiles) {
        if (!settings.isEnabled() || settings.getUrl() == null || settings.getUrl().isEmpty()) {
            return;
        }

        String message = format(settings.getEndMessage())
            .replace("{player}", playerName)
            .replace("{profile}", profiles)
            .replace("{time}", TIME_FORMAT.format(Instant.now()));

        sendWebhook(message, 0xFF0000, playerUuid);
    }

    public void sendActionLog(String playerName, UUID playerUuid, String action, String details) {
        if (!settings.isEnabled() || !settings.isActionLog() || settings.getUrl() == null || settings.getUrl().isEmpty()) {
            return;
        }

        pendingNames.put(playerUuid, playerName);
        pendingActions.computeIfAbsent(playerUuid, k -> new ArrayList<>())
            .add(action + ": " + details);

        int maxEntries = Math.max(1, settings.getActionBatchMaxEntries());
        if (pendingActions.get(playerUuid).size() >= maxEntries) {
            aggregator.execute(() -> flushActions(playerUuid));
            return;
        }

        int delay = Math.max(1, settings.getActionBatchWindowSeconds());
        aggregator.schedule(() -> flushActions(playerUuid), delay, TimeUnit.SECONDS);
    }

    public void sendSimple(String content, int color) {
        sendWebhook(content, color, null);
    }

    private void sendWebhook(String content, int color, UUID playerUuid) {
        CompletableFuture.runAsync(() -> {
            SendOutcome outcome = postWebhook(settings.getUrl(), content, color, playerUuid, false);
            if (outcome.rateLimited()) {
                Logger.warn("Discord webhook is rate limited; message was dropped");
            } else if (!outcome.success()) {
                Logger.warn("Discord webhook returned non-success status");
            }
        });
    }

    /**
     * Support both real newlines and literal "\n" sequences in config values.
     */
    private String format(String template) {
        if (template == null) {
            return "";
        }
        return template.replace("\\n", "\n");
    }

    private void flushActions(UUID playerUuid) {
        List<String> lines = pendingActions.remove(playerUuid);
        if (lines == null || lines.isEmpty()) {
            return;
        }
        String playerName = pendingNames.getOrDefault(playerUuid, "Unknown");
        String joinedDetails = String.join("\n", lines);
        String message = format(settings.getActionMessage())
            .replace("{player}", playerName)
            .replace("{action}", "Actions")
            .replace("{details}", joinedDetails)
            .replace("{time}", TIME_FORMAT.format(Instant.now()));
        sendOrEditAction(playerUuid, lines, message, 0xFFFF00);
    }

    public void shutdown() {
        aggregator.shutdownNow();
    }

    private void sendOrEditAction(UUID playerUuid, List<String> lines, String content, int color) {
        if (!settings.isActionBatchEdit()) {
            SendOutcome outcome = postWebhook(settings.getUrl(), content, color, playerUuid, false);
            if (outcome.rateLimited()) {
                requeueLines(playerUuid, lines);
            }
            return;
        }

        String messageId = lastActionMessageId.get(playerUuid);
        if (messageId != null) {
            SendOutcome outcome = tryEditWebhook(messageId, content, color, playerUuid);
            if (outcome.success()) {
                return;
            }
            if (!outcome.rateLimited()) {
                lastActionMessageId.remove(playerUuid);
            } else {
                requeueLines(playerUuid, lines);
                return;
            }
        }

        SendOutcome outcome = postWebhook(settings.getUrl(), content, color, playerUuid, true);
        if (outcome.success() && outcome.messageId() != null) {
            lastActionMessageId.put(playerUuid, outcome.messageId());
        } else if (outcome.rateLimited()) {
            requeueLines(playerUuid, lines);
        }
    }

    private SendOutcome tryEditWebhook(String messageId, String content, int color, UUID playerUuid) {
        try {
            String baseUrl = settings.getUrl();
            JsonObject json = buildPayload(content, color, playerUuid);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/messages/" + messageId))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("User-Agent", "DoubleLife/1.0")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
                .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            if (code == 429) {
                return SendOutcome.rateLimitedResult();
            }
            return SendOutcome.result(code >= 200 && code < 300, false, null);
        } catch (Exception e) {
            Logger.warn("Failed to edit Discord webhook: " + e.getMessage());
            return SendOutcome.failureResult();
        }
    }

    private SendOutcome postWebhook(String baseUrl, String content, int color, UUID playerUuid, boolean captureId) {
        try {
            String target = captureId
                ? (baseUrl.contains("?") ? baseUrl + "&wait=true" : baseUrl + "?wait=true")
                : baseUrl;

            JsonObject payload = buildPayload(content, color, playerUuid);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(target))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("User-Agent", "DoubleLife/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            if (code == 429) {
                return SendOutcome.rateLimitedResult();
            }

            if (code >= 200 && code < 300) {
                String id = null;
                if (captureId) {
                    JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (obj.has("id")) {
                        id = obj.get("id").getAsString();
                    }
                }
                return SendOutcome.result(true, false, id);
            }

            Logger.warn("Discord webhook returned code: " + code);
            return SendOutcome.failureResult();
        } catch (Exception e) {
            Logger.error("Failed to send Discord webhook: " + e.getMessage());
            return SendOutcome.failureResult();
        }
    }

    private JsonObject buildPayload(String content, int color, UUID playerUuid) {
        JsonObject json = new JsonObject();
        json.addProperty("content", (String) null);
        json.addProperty("username", "DoubleLife System");
        json.addProperty("avatar_url", avatarUrl(playerUuid));

        JsonObject embed = new JsonObject();
        embed.addProperty("description", content);
        embed.addProperty("color", color);
        embed.addProperty("timestamp", Instant.now().toString());

        JsonObject footer = new JsonObject();
        footer.addProperty("text", "DoubleLife Plugin");
        embed.add("footer", footer);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        json.add("embeds", embeds);
        return json;
    }

    private void requeueLines(UUID playerUuid, List<String> lines) {
        Logger.warn("Discord webhook hit rate limit; merging action logs and retrying soon");
        pendingActions.compute(playerUuid, (id, existing) -> {
            if (existing == null) {
                return new ArrayList<>(lines);
            }
            existing.addAll(lines);
            return existing;
        });
        int retryDelay = Math.max(3, settings.getActionBatchWindowSeconds());
        aggregator.schedule(() -> flushActions(playerUuid), retryDelay, TimeUnit.SECONDS);
    }

    private record SendOutcome(boolean success, boolean rateLimited, String messageId) {
        static SendOutcome rateLimitedResult() {
            return new SendOutcome(false, true, null);
        }

        static SendOutcome failureResult() {
            return new SendOutcome(false, false, null);
        }

        static SendOutcome result(boolean success, boolean rateLimited, String messageId) {
            return new SendOutcome(success, rateLimited, messageId);
        }
    }

    private String avatarUrl(UUID playerUuid) {
        if (playerUuid == null) {
            return "https://www.minecraft.net/content/dam/games/minecraft/key-art/CC-Update-Part-II_1024x576.jpg";
        }
        return "https://mc-heads.net/avatar/" + playerUuid + "?size=128&overlay";
    }
}
