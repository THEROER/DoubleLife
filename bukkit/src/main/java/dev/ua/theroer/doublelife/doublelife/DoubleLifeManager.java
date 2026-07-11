package dev.ua.theroer.doublelife.doublelife;

import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import dev.ua.theroer.doublelife.config.DoubleLifeProfile;
import dev.ua.theroer.doublelife.config.PlayerSetting;
import dev.ua.theroer.doublelife.config.SecondLifeMode;
import dev.ua.theroer.doublelife.config.SwapSettings;
import dev.ua.theroer.doublelife.doublelife.storage.FileKitStorage;
import dev.ua.theroer.doublelife.doublelife.storage.FilePersonaStorage;
import dev.ua.theroer.doublelife.doublelife.storage.InventoryStorage;
import dev.ua.theroer.doublelife.doublelife.storage.ItemSerialization;
import dev.ua.theroer.doublelife.doublelife.storage.FilePlayerSettingsStorage;
import dev.ua.theroer.doublelife.doublelife.storage.KitStorage;
import dev.ua.theroer.doublelife.doublelife.storage.PersonaStorage;
import dev.ua.theroer.doublelife.doublelife.storage.PlayerSettingsStorage;
import dev.ua.theroer.doublelife.doublelife.webhook.ActionCategory;
import dev.ua.theroer.doublelife.doublelife.webhook.WebhookManager;
import dev.ua.theroer.magicutils.Logger;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
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
    private final PersonaStorage personaStorage;
    private final KitStorage kitStorage;
    private final PlayerSettingsStorage playerSettingsStorage;
    private final WebhookManager webhookManager;
    private final DoubleLifeBossBarManager bossBarManager;
    private final LuckPermsHandler luckPermsHandler;
    private final Logger logger;

    public DoubleLifeManager(DoubleLifePlugin plugin, DoubleLifeConfig config, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getMLogger();
        this.activeSessions = new ConcurrentHashMap<>();
        ItemSerialization itemSerialization = new ItemSerialization(logger);
        this.inventoryStorage = new InventoryStorage(plugin, config.getStoragePath(), itemSerialization);
        this.personaStorage = new FilePersonaStorage(plugin, config.getStoragePath(), itemSerialization);
        this.kitStorage = new FileKitStorage(plugin, config.getStoragePath(), itemSerialization);
        this.playerSettingsStorage = new FilePlayerSettingsStorage(plugin, config.getStoragePath());
        this.webhookManager = new WebhookManager(logger, config.getWebhooks());
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
            logger.error().to(player).send(reason);
            logger.warn("Failed to start DoubleLife for " + player.getName() + ": " + reason);
            return StartResult.error(reason);
        }

        if (activeSessions.containsKey(player.getUniqueId())) {
            String reason = "You already have an active DoubleLife session!";
            logger.error().to(player).send(reason);
            logger.warn("Failed to start DoubleLife for " + player.getName() + ": " + reason);
            return StartResult.error(reason);
        }

        Set<String> applicableProfiles = getApplicableProfiles(player);
        if (applicableProfiles.isEmpty()) {
            String reason = "No DoubleLife profiles available for your rank.";
            logger.error().to(player).send(reason);
            logger.warn("Failed to start DoubleLife for " + player.getName() + ": " + reason);
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
        applySecondLife(player, session);
        applyDoubleLifePermissions(player, session);

        activeSessions.put(player.getUniqueId(), session);

        if (session.getDuration() > 0) {
            session.scheduleEnd(() -> endDoubleLife(player), plugin);
        }

        bossBarManager.createBossBar(player, session);

        logger.success().to(player).send(
            "DoubleLife activated! Duration: " + session.getFormattedRemainingTime() + " "
                + "<dark_gray>[<red><hover:show_text:'End your DoubleLife session'>"
                + "<click:run_command:'/dl stop'>end</click></hover></red>]</dark_gray>"
        );
        String profiles = String.join(", ", applicableProfiles);
        webhookManager.sendStartNotification(player.getName(), player.getUniqueId(), profiles, session.getFormattedRemainingTime());

        runCommands(config.getCommands().getAfterStart(), player, session);
        runCommands(collectProfileCommands(session, p -> p.getCommands().getAfterStart()), player, session);

        logger.info("DoubleLife started for " + player.getName() + " with profiles: " + profiles);
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
        saveSecondLifeIfPersona(player, session);
        restorePlayerState(player, session);
        removeDoubleLifePermissions(player, session);

        logger.info().to(player).send("DoubleLife ended.");
        webhookManager.sendEndNotification(player.getName(), session.getPlayerUuid(), String.join(", ", session.getActiveProfiles()));

        runCommands(config.getCommands().getAfterEnd(), player, session);
        runCommands(collectProfileCommands(session, p -> p.getCommands().getAfterEnd()), player, session);

        logger.info("DoubleLife ended for " + player.getName());
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

    /**
     * Installs the second-life state for a starting session according to its
     * resolved mode:
     * <ul>
     *   <li>EMPTY  — wipe to an empty second inventory.</li>
     *   <li>KIT    — fresh copy of the profile's seed kit every time.</li>
     *   <li>PERSONA — the stored second character, seeded from the kit on the very
     *       first entry; always-give kits then top up any missing items.</li>
     * </ul>
     */
    private void applySecondLife(Player player, DoubleLifeSession session) {
        player.getEnderChest().clear();
        player.setExp(0);
        player.setLevel(0);
        player.setHealth(clampHealth(player, 20.0));
        player.setFoodLevel(20);
        player.getInventory().clear();

        SecondLifeMode mode = resolveSecondLifeMode(session);
        if (mode == SecondLifeMode.KIT) {
            applyKit(player, seedKitName(session));
        } else if (mode == SecondLifeMode.PERSONA) {
            PersonaStorage.PersonaData persona = personaStorage.load(player.getUniqueId());
            if (persona != null) {
                SwapSettings swap = config.getSecondLife().getSwap();
                if (swap.isInventory()) {
                    player.getInventory().setContents(persona.inventory);
                    player.getInventory().setArmorContents(persona.armor);
                }
                if (swap.isEnderChest()) {
                    player.getEnderChest().setContents(persona.enderChest);
                }
                if (swap.isExperience()) {
                    player.setExp(persona.exp);
                    player.setLevel(persona.level);
                }
                if (swap.isHealthFood()) {
                    player.setHealth(clampHealth(player, persona.health));
                    player.setFoodLevel(persona.foodLevel);
                }
            } else {
                // First time on this server: seed the persona from the kit.
                applyKit(player, seedKitName(session));
            }
            topUpAlwaysGive(player, session);
        }
        // EMPTY leaves the cleared inventory as-is.
    }

    /** Adds a kit's contents to the player without removing what they already hold. */
    private void applyKit(Player player, String kitName) {
        if (kitName == null || kitName.isEmpty()) {
            return;
        }
        KitStorage.KitData kit = kitStorage.load(kitName);
        if (kit == null) {
            logger.warn("DoubleLife kit '" + kitName + "' not found; skipping");
            return;
        }
        giveItems(player, kit.inventory);
        for (org.bukkit.inventory.ItemStack armor : kit.armor) {
            if (armor != null) {
                player.getInventory().addItem(armor);
            }
        }
    }

    /**
     * Ensures every item from the session's always-give kits is present, adding
     * only what the player is missing so it isn't duplicated each entry.
     */
    private void topUpAlwaysGive(Player player, DoubleLifeSession session) {
        for (String kitName : alwaysGiveKitNames(session)) {
            KitStorage.KitData kit = kitStorage.load(kitName);
            if (kit == null) {
                logger.warn("DoubleLife always-give kit '" + kitName + "' not found; skipping");
                continue;
            }
            for (org.bukkit.inventory.ItemStack item : kit.inventory) {
                if (item != null && !player.getInventory().containsAtLeast(item, item.getAmount())) {
                    player.getInventory().addItem(item.clone());
                }
            }
        }
    }

    private void giveItems(Player player, org.bukkit.inventory.ItemStack[] contents) {
        for (org.bukkit.inventory.ItemStack item : contents) {
            if (item != null) {
                player.getInventory().addItem(item.clone());
            }
        }
    }

    private String seedKitName(DoubleLifeSession session) {
        for (String profileName : session.getActiveProfiles()) {
            DoubleLifeProfile profile = config.getProfiles().get(profileName);
            if (profile != null && profile.getSeedKit() != null && !profile.getSeedKit().isEmpty()) {
                return profile.getSeedKit();
            }
        }
        return null;
    }

    private java.util.List<String> alwaysGiveKitNames(DoubleLifeSession session) {
        java.util.List<String> names = new ArrayList<>();
        for (String profileName : session.getActiveProfiles()) {
            DoubleLifeProfile profile = config.getProfiles().get(profileName);
            if (profile != null && profile.getAlwaysGive() != null) {
                names.addAll(profile.getAlwaysGive());
            }
        }
        return names;
    }

    public KitStorage getKitStorage() {
        return kitStorage;
    }

    /**
     * Resolves the effective second-life mode for a session: the strongest mode
     * declared by any active profile (PERSONA > KIT > EMPTY), else the global
     * default.
     */
    private SecondLifeMode resolveSecondLifeMode(DoubleLifeSession session) {
        SecondLifeMode resolved = null;
        for (String profileName : session.getActiveProfiles()) {
            DoubleLifeProfile profile = config.getProfiles().get(profileName);
            if (profile == null || profile.getSecondLifeMode() == null) {
                continue;
            }
            resolved = strongerMode(resolved, profile.getSecondLifeMode());
        }
        if (resolved != null) {
            return resolved;
        }
        return config.getSecondLife().getMode();
    }

    private SecondLifeMode strongerMode(SecondLifeMode current, SecondLifeMode candidate) {
        if (current == null) {
            return candidate;
        }
        return candidate.ordinal() > current.ordinal() ? candidate : current;
    }

    /**
     * When leaving a PERSONA session, captures the player's current inventory and
     * armour back into their stored second character so it carries over.
     */
    private void saveSecondLifeIfPersona(Player player, DoubleLifeSession session) {
        if (resolveSecondLifeMode(session) != SecondLifeMode.PERSONA) {
            return;
        }
        SwapSettings swap = config.getSecondLife().getSwap();
        PersonaStorage.PersonaData persona = new PersonaStorage.PersonaData();
        if (swap.isInventory()) {
            persona.inventory = player.getInventory().getContents().clone();
            persona.armor = player.getInventory().getArmorContents().clone();
        }
        if (swap.isEnderChest()) {
            persona.enderChest = player.getEnderChest().getContents().clone();
        }
        if (swap.isExperience()) {
            persona.exp = player.getExp();
            persona.level = player.getLevel();
        }
        if (swap.isHealthFood()) {
            persona.health = player.getHealth();
            persona.foodLevel = player.getFoodLevel();
        }
        personaStorage.save(player.getUniqueId(), persona);
    }

    private void restorePlayerState(Player player, DoubleLifeSession session) {
        player.getInventory().setContents(session.getSavedInventory());
        player.getInventory().setArmorContents(session.getSavedArmor());
        player.getEnderChest().setContents(session.getSavedEnderChest());
        player.setGameMode(session.getSavedGameMode());
        if (session.getSavedLocation() != null && session.getSavedLocation().getWorld() != null) {
            player.teleport(session.getSavedLocation());
        }
        player.setExp(session.getSavedExp());
        player.setLevel(session.getSavedLevel());
        player.setHealth(clampHealth(player, session.getSavedHealth()));
        player.setFoodLevel(session.getSavedFoodLevel());

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

    private double clampHealth(Player player, double desired) {
        double max = 20.0;
        Attribute maxHealthAttr = resolveMaxHealthAttribute();
        if (maxHealthAttr != null && player.getAttribute(maxHealthAttr) != null) {
            max = player.getAttribute(maxHealthAttr).getValue();
        }
        if (max < 0) {
            max = 0;
        }
        return Math.max(0.0, Math.min(desired, max));
    }

    private Attribute resolveMaxHealthAttribute() {
        Attribute attr = resolveAttributeByField("GENERIC_MAX_HEALTH");
        if (attr != null) {
            return attr;
        }
        attr = resolveAttributeByField("MAX_HEALTH");
        if (attr != null) {
            return attr;
        }
        return null;
    }

    private Attribute resolveAttributeByField(String fieldName) {
        try {
            return (Attribute) Attribute.class.getField(fieldName).get(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            return null;
        }
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
                        logger.info("DoubleLife session expired offline for " + session.getPlayerName() + "; inventory will be restored on next login");
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

    /**
     * Whether a player currently in their second life is protected from hostile
     * mobs. Player-level overrides land in a later stage; for now this is the
     * global default gated on having an active session.
     */
    public boolean isMobProtected(UUID playerUuid) {
        if (!activeSessions.containsKey(playerUuid)) {
            return false;
        }
        var mobProtection = config.getSecondLife().getMobProtection();
        if (mobProtection.isPlayerAdjustable()) {
            Boolean override = playerSettingsStorage.get(playerUuid, PlayerSetting.MOB_PROTECTION);
            if (override != null) {
                return override;
            }
        }
        return mobProtection.isEnabled();
    }

    /** Whether a player-adjustable setting can currently be changed by players. */
    public boolean isSettingAdjustable(PlayerSetting setting) {
        if (setting == PlayerSetting.MOB_PROTECTION) {
            return config.getSecondLife().getMobProtection().isPlayerAdjustable();
        }
        return false;
    }

    /**
     * The player's effective value for a setting: their explicit choice if any,
     * otherwise the server default.
     */
    public boolean effectiveSetting(UUID playerUuid, PlayerSetting setting) {
        Boolean override = playerSettingsStorage.get(playerUuid, setting);
        if (override != null) {
            return override;
        }
        return serverDefault(setting);
    }

    /** Whether the player has an explicit choice (vs inheriting the default). */
    public boolean hasSettingOverride(UUID playerUuid, PlayerSetting setting) {
        return playerSettingsStorage.get(playerUuid, setting) != null;
    }

    public void setPlayerSetting(UUID playerUuid, PlayerSetting setting, Boolean value) {
        playerSettingsStorage.set(playerUuid, setting, value);
    }

    private boolean serverDefault(PlayerSetting setting) {
        if (setting == PlayerSetting.MOB_PROTECTION) {
            return config.getSecondLife().getMobProtection().isEnabled();
        }
        return false;
    }

    /**
     * Whether a player in their second life keeps their inventory on death,
     * so unique items in a persona can't be duplicated by dying.
     */
    public boolean keepsInventoryOnDeath(UUID playerUuid) {
        if (!activeSessions.containsKey(playerUuid)) {
            return false;
        }
        return config.getSecondLife().isDeathKeepsInventory();
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
            logger.success().to(player).send("Your DoubleLife session has been restored. Time remaining: " + savedSession.getFormattedRemainingTime());
            logger.info("Restored active DoubleLife session for " + player.getName());
        } else {
            logger.info("Expired DoubleLife session found for " + player.getName() + ", restoring inventory only");
            restorePlayerState(player, savedSession);
            removeDoubleLifePermissions(player, savedSession);
            inventoryStorage.deleteSession(player.getUniqueId());
            logger.warn().to(player).send("Your DoubleLife session expired while you were offline. Your inventory has been restored.");
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

    public void logAction(Player player, ActionCategory category, String action, String details) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            webhookManager.sendActionLog(player.getName(), player.getUniqueId(), category, action, details);
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
