package dev.ua.theroer.doublelife.doublelife.webhook;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ua.theroer.doublelife.config.WebhookSettings;
import dev.ua.theroer.magicutils.Logger;

import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
            try {
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

                URL url = new URI(settings.getUrl()).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "DoubleLife/1.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 204) {
                    Logger.warn().send("Discord webhook returned code: " + responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                Logger.error().send("Failed to send Discord webhook: " + e.getMessage());
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
        sendOrEditAction(playerUuid, message, 0xFFFF00);
    }

    public void shutdown() {
        aggregator.shutdownNow();
    }

    private void sendOrEditAction(UUID playerUuid, String content, int color) {
        if (!settings.isActionBatchEdit()) {
            sendWebhook(content, color, playerUuid);
            return;
        }

        String messageId = lastActionMessageId.get(playerUuid);
        if (messageId != null) {
            boolean edited = tryEditWebhook(messageId, content, color, playerUuid);
            if (edited) {
                return;
            }
            lastActionMessageId.remove(playerUuid);
        }

        String newId = sendWebhookCaptureId(content, color, playerUuid);
        if (newId != null) {
            lastActionMessageId.put(playerUuid, newId);
        }
    }

    private boolean tryEditWebhook(String messageId, String content, int color, UUID playerUuid) {
        try {
            String baseUrl = settings.getUrl();
            URL url = new URI(baseUrl + "/messages/" + messageId).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PATCH");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "DoubleLife/1.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            JsonObject json = buildPayload(content, color, playerUuid);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            connection.disconnect();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            Logger.warn().send("Failed to edit Discord webhook: " + e.getMessage());
            return false;
        }
    }

    private String sendWebhookCaptureId(String content, int color, UUID playerUuid) {
        try {
            String baseUrl = settings.getUrl();
            String withWait = baseUrl.contains("?") ? baseUrl + "&wait=true" : baseUrl + "?wait=true";
            URL url = new URI(withWait).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "DoubleLife/1.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            JsonObject json = buildPayload(content, color, playerUuid);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = connection.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    if (obj.has("id")) {
                        return obj.get("id").getAsString();
                    }
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            Logger.warn().send("Failed to send Discord webhook (capture id): " + e.getMessage());
        }
        return null;
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

    private String avatarUrl(UUID playerUuid) {
        if (playerUuid == null) {
            return "https://www.minecraft.net/content/dam/games/minecraft/key-art/CC-Update-Part-II_1024x576.jpg";
        }
        return "https://mc-heads.net/avatar/" + playerUuid + "?size=128&overlay";
    }
}
