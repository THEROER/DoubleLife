package dev.ua.theroer.doublelife.doublelife;

import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import dev.ua.theroer.doublelife.config.DoubleLifeProfile;
import dev.ua.theroer.doublelife.doublelife.storage.InventoryStorage;
import dev.ua.theroer.doublelife.doublelife.webhook.WebhookManager;
import dev.ua.theroer.magicutils.Logger;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.function.Function;

public class DoubleLifeManager {

    private final DoubleLifePlugin plugin;
    private final DoubleLifeConfig config;
    private final Map<UUID, DoubleLifeSession> activeSessions;
    private final InventoryStorage inventoryStorage;
    private final WebhookManager webhookManager;
    private final DoubleLifeBossBarManager bossBarManager;
    private final LuckPermsHandler luckPermsHandler;

    public DoubleLifeManager(DoubleLifePlugin plugin, DoubleLifeConfig config, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.config = config;
        this.activeSessions = new ConcurrentHashMap<>();
        this.inventoryStorage = new InventoryStorage(plugin, config.getStoragePath());
        this.webhookManager = new WebhookManager(config.getWebhooks());
        this.bossBarManager = new DoubleLifeBossBarManager(plugin, config);
        this.luckPermsHandler = new LuckPermsHandler(luckPerms);

        startSessionChecker();
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public StartResult startDoubleLife(Player player, int durationOverride) {
        if (!config.isEnabled()) {
            String reason = "DoubleLife is disabled.";
            player.sendMessage("§c" + reason);
            Logger.warn().send("Failed to start DoubleLife for " + player.getName() + ": " + reason);
            return StartResult.error(reason);
        }

        if (activeSessions.containsKey(player.getUniqueId())) {
            String reason = "You already have an active DoubleLife session!";
            player.sendMessage("§c" + reason);
            Logger.warn().send("Failed to start DoubleLife for " + player.getName() + ": " + reason);
            return StartResult.error(reason);
        }

        Set<String> applicableProfiles = getApplicableProfiles(player);
        if (applicableProfiles.isEmpty()) {
            String reason = "No DoubleLife profiles available for your rank.";
            player.sendMessage("§c" + reason);
            Logger.warn().send("Failed to start DoubleLife for " + player.getName() + ": " + reason);
            return StartResult.error(reason);
        }

        int duration = durationOverride > 0 ? durationOverride : resolveDuration(applicableProfiles);

        DoubleLifeSession session = new DoubleLifeSession(
            player.getUniqueId(),
            player.getName(),
            duration,
            applicableProfiles
        );

        runCommands(config.getCommands().getBeforeStart(), player, session);
        runCommands(collectProfileCommands(session, p -> p.getCommands().getBeforeStart()), player, session);

        savePlayerState(player, session);
        clearPlayer(player);
        applyDoubleLifePermissions(player, session);

        activeSessions.put(player.getUniqueId(), session);

        if (session.getDuration() > 0) {
            session.scheduleEnd(() -> endDoubleLife(player), plugin);
        }

        bossBarManager.createBossBar(player, session);

        player.sendMessage("§aDoubleLife activated! Duration: " + session.getFormattedRemainingTime());
        String profiles = String.join(", ", applicableProfiles);
        webhookManager.sendStartNotification(player.getName(), player.getUniqueId(), profiles, session.getFormattedRemainingTime());

        runCommands(config.getCommands().getAfterStart(), player, session);
        runCommands(collectProfileCommands(session, p -> p.getCommands().getAfterStart()), player, session);

        Logger.info().send("DoubleLife started for " + player.getName() + " with profiles: " + profiles);
        return StartResult.ok();
    }

    public boolean endDoubleLife(Player player) {
        DoubleLifeSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }

        runCommands(config.getCommands().getBeforeEnd(), player, session);
        runCommands(collectProfileCommands(session, p -> p.getCommands().getBeforeEnd()), player, session);

        session.end();
        bossBarManager.removeBossBar(player.getUniqueId());
        restorePlayerState(player, session);
        removeDoubleLifePermissions(player, session);

        player.sendMessage("§cDoubleLife ended.");
        webhookManager.sendEndNotification(player.getName(), session.getPlayerUuid(), String.join(", ", session.getActiveProfiles()));

        runCommands(config.getCommands().getAfterEnd(), player, session);
        runCommands(collectProfileCommands(session, p -> p.getCommands().getAfterEnd()), player, session);

        Logger.info().send("DoubleLife ended for " + player.getName());
        return true;
    }

    private Set<String> getApplicableProfiles(Player player) {
        Set<String> playerGroups = luckPermsHandler.getPlayerGroups(player.getUniqueId());

        return config.getProfiles().entrySet().stream()
            .filter(entry -> playerGroups.contains(entry.getValue().getGroupName()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    private int resolveDuration(Set<String> profiles) {
        int duration = 0;
        for (String name : profiles) {
            DoubleLifeProfile profile = config.getProfiles().get(name);
            if (profile == null) {
                continue;
            }
            if (profile.getDuration() == 0) {
                return 0;
            }
            duration = Math.max(duration, profile.getDuration());
        }
        if (duration == 0) {
            duration = config.getDefaultDuration();
        }
        return duration;
    }

    private void savePlayerState(Player player, DoubleLifeSession session) {
        session.setSavedInventory(player.getInventory().getContents().clone());
        session.setSavedArmor(player.getInventory().getArmorContents().clone());
        session.setSavedEnderChest(player.getEnderChest().getContents().clone());
        session.setSavedGameMode(player.getGameMode());
        session.setSavedLocation(player.getLocation().clone());
        session.setSavedHealth(player.getHealth());
        session.setSavedFoodLevel(player.getFoodLevel());
        session.setSavedExp(player.getExp());
        session.setSavedLevel(player.getLevel());

        List<String> groups = luckPermsHandler.getPlayerGroupsList(player.getUniqueId());
        session.setOriginalGroups(groups);

        inventoryStorage.saveSession(session);
    }

    private void clearPlayer(Player player) {
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
    }

    private void restorePlayerState(Player player, DoubleLifeSession session) {
        player.getInventory().setContents(session.getSavedInventory());
        player.getInventory().setArmorContents(session.getSavedArmor());
        player.getEnderChest().setContents(session.getSavedEnderChest());
        player.setGameMode(session.getSavedGameMode());
        if (session.getSavedLocation() != null && session.getSavedLocation().getWorld() != null) {
            player.teleport(session.getSavedLocation());
        }
        player.setHealth(session.getSavedHealth());
        player.setFoodLevel(session.getSavedFoodLevel());
        player.setExp(session.getSavedExp());
        player.setLevel(session.getSavedLevel());

        inventoryStorage.deleteSession(player.getUniqueId());
    }

    private void applyDoubleLifePermissions(Player player, DoubleLifeSession session) {
        String uniqueGroupName = config.getTemporaryGroup() + "-" + player.getName();
        luckPermsHandler.createTemporaryGroup(uniqueGroupName, session.getDuration());

        List<String> allPermissions = new ArrayList<>();
        for (String profileName : session.getActiveProfiles()) {
            DoubleLifeProfile profile = config.getProfiles().get(profileName);
            if (profile != null && profile.getPermissions() != null) {
                allPermissions.addAll(profile.getPermissions());
            }
        }

        if (!allPermissions.isEmpty()) {
            luckPermsHandler.addPermissionsToGroup(uniqueGroupName, allPermissions, session.getDuration());
        }

        luckPermsHandler.addTemporaryGroup(player.getUniqueId(), uniqueGroupName, session.getDuration());
        session.setTemporaryGroupName(uniqueGroupName);
    }

    private void removeDoubleLifePermissions(Player player, DoubleLifeSession session) {
        String groupName = session.getTemporaryGroupName();
        if (groupName == null || groupName.isEmpty()) {
            groupName = config.getTemporaryGroup() + "-" + player.getName();
        }

        luckPermsHandler.removeTemporaryGroup(player.getUniqueId(), groupName);
        luckPermsHandler.deleteGroup(groupName);
        luckPermsHandler.clearTemporaryNodes(player.getUniqueId());
    }

    private void startSessionChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, DoubleLifeSession> entry : new HashSet<>(activeSessions.entrySet())) {
                DoubleLifeSession session = entry.getValue();
                if (session.isExpired()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        endDoubleLife(player);
                    } else {
                        session.end();
                        inventoryStorage.saveSession(session);
                        activeSessions.remove(entry.getKey());
                        Logger.info().send("DoubleLife session expired offline for " + session.getPlayerName() + "; inventory will be restored on next login");
                    }
                }
            }
        }, 20L, 20L);
    }

    public DoubleLifeSession getSession(UUID playerUuid) {
        return activeSessions.get(playerUuid);
    }

    public Collection<DoubleLifeSession> getActiveSessions() {
        return activeSessions.values();
    }

    public boolean hasActiveSession(UUID playerUuid) {
        return activeSessions.containsKey(playerUuid);
    }

    public void handlePlayerJoin(Player player) {
        DoubleLifeSession savedSession = inventoryStorage.loadSession(player.getUniqueId());
        if (savedSession == null) {
            return;
        }

        if (!savedSession.isExpired()) {
            activeSessions.put(player.getUniqueId(), savedSession);
            applyDoubleLifePermissions(player, savedSession);

            if (savedSession.getDuration() > 0) {
                savedSession.scheduleEnd(() -> endDoubleLife(player), plugin);
            }

            bossBarManager.createBossBar(player, savedSession);
            player.sendMessage("§aYour DoubleLife session has been restored. Time remaining: " + savedSession.getFormattedRemainingTime());
            Logger.info().send("Restored active DoubleLife session for " + player.getName());
        } else {
            Logger.info().send("Expired DoubleLife session found for " + player.getName() + ", restoring inventory only");
            restorePlayerState(player, savedSession);
            removeDoubleLifePermissions(player, savedSession);
            inventoryStorage.deleteSession(player.getUniqueId());
            player.sendMessage("§cYour DoubleLife session expired while you were offline. Your inventory has been restored.");
            webhookManager.sendEndNotification(player.getName(), player.getUniqueId(), "Session expired (offline)");
        }
    }

    public void handlePlayerQuit(Player player) {
        DoubleLifeSession session = activeSessions.get(player.getUniqueId());
        if (session != null && session.isActive()) {
            inventoryStorage.saveSession(session);
            bossBarManager.removeBossBar(player.getUniqueId());
        }
    }

    public void logAction(Player player, String action, String details) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            webhookManager.sendActionLog(player.getName(), player.getUniqueId(), action, details);
        }
    }

    public void shutdown() {
        bossBarManager.removeAllBossBars();
        for (DoubleLifeSession session : activeSessions.values()) {
            inventoryStorage.saveSession(session);
        }
        webhookManager.shutdown();
    }

    public record StartResult(boolean success, String reason) {
        public static StartResult ok() {
            return new StartResult(true, null);
        }

        public static StartResult error(String reason) {
            return new StartResult(false, reason);
        }
    }

    private void runCommands(List<String> commands, Player player, DoubleLifeSession session) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        String profiles = session != null ? String.join(",", session.getActiveProfiles()) : "";
        String duration = session != null ? String.valueOf(session.getDuration()) : "0";
        String remaining = session != null ? String.valueOf(session.getRemainingSeconds()) : duration;

        for (String raw : commands) {
            String cmd = raw
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{profiles}", profiles)
                .replace("{duration}", duration)
                .replace("{remaining}", remaining);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }

    private List<String> collectProfileCommands(DoubleLifeSession session, Function<DoubleLifeProfile, List<String>> extractor) {
        List<String> list = new ArrayList<>();
        if (session == null) {
            return list;
        }
        for (String profileName : session.getActiveProfiles()) {
            DoubleLifeProfile profile = config.getProfiles().get(profileName);
            if (profile == null) {
                continue;
            }
            List<String> cmds = extractor.apply(profile);
            if (cmds != null && !cmds.isEmpty()) {
                list.addAll(cmds);
            }
        }
        return list;
    }
}
