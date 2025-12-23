package dev.ua.theroer.doublelife.doublelife.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeSession;
import dev.ua.theroer.magicutils.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class InventoryStorage {

    private final File storageDir;
    private final Gson gson;
    private final Logger logger;

    public InventoryStorage(DoubleLifePlugin plugin, String storagePath) {
        this.storageDir = new File(plugin.getDataFolder(), storagePath);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        this.logger = plugin.getMLogger();
    }

    public void saveSession(DoubleLifeSession session) {
        File sessionFile = new File(storageDir, session.getPlayerUuid() + ".json");
        try {
            SessionData data = new SessionData();
            data.playerUuid = session.getPlayerUuid().toString();
            data.playerName = session.getPlayerName();
            data.startTime = session.getStartTime().toEpochMilli();
            data.duration = session.getDuration();
            data.activeProfiles = session.getActiveProfiles().toArray(new String[0]);

            data.savedInventory = serializeInventory(session.getSavedInventory());
            data.savedArmor = serializeInventory(session.getSavedArmor());
            data.savedEnderChest = serializeInventory(session.getSavedEnderChest());
            data.savedGameMode = session.getSavedGameMode().name();
            data.savedLocation = LocationData.fromLocation(session.getSavedLocation());
            data.savedHealth = session.getSavedHealth();
            data.savedFoodLevel = session.getSavedFoodLevel();
            data.savedExp = session.getSavedExp();
            data.savedLevel = session.getSavedLevel();
            data.originalGroups = session.getOriginalGroups().toArray(new String[0]);
            data.temporaryGroupName = session.getTemporaryGroupName();

            try (Writer writer = Files.newBufferedWriter(sessionFile.toPath())) {
                gson.toJson(data, writer);
            }
            logger.debug("Saved DoubleLife session for " + session.getPlayerName());
        } catch (Exception e) {
            logger.error("Failed to save DoubleLife session for " + session.getPlayerName() + ": " + e.getMessage());
        }
    }

    public DoubleLifeSession loadSession(UUID playerUuid) {
        File sessionFile = new File(storageDir, playerUuid + ".json");
        if (!sessionFile.exists()) {
            return null;
        }

        try {
            SessionData data;
            try (Reader reader = Files.newBufferedReader(sessionFile.toPath())) {
                data = gson.fromJson(reader, SessionData.class);
            }

            DoubleLifeSession session = new DoubleLifeSession(
                UUID.fromString(data.playerUuid),
                data.playerName,
                Instant.ofEpochMilli(data.startTime),
                data.duration,
                data.activeProfiles != null ? java.util.Set.of(data.activeProfiles) : java.util.Set.of()
            );

            session.setSavedInventory(deserializeInventory(data.savedInventory));
            session.setSavedArmor(deserializeInventory(data.savedArmor));
            session.setSavedEnderChest(deserializeInventory(data.savedEnderChest));
            session.setSavedGameMode(data.savedGameMode != null ? GameMode.valueOf(data.savedGameMode) : GameMode.SURVIVAL);
            session.setSavedLocation(data.savedLocation != null ? data.savedLocation.toLocation() : null);
            session.setSavedHealth(data.savedHealth);
            session.setSavedFoodLevel(data.savedFoodLevel);
            session.setSavedExp(data.savedExp);
            session.setSavedLevel(data.savedLevel);
            session.setOriginalGroups(java.util.Arrays.asList(data.originalGroups));
            session.setTemporaryGroupName(data.temporaryGroupName);

            logger.debug("Loaded DoubleLife session for " + data.playerName);
            return session;
        } catch (Exception e) {
            logger.error("Failed to load DoubleLife session for " + playerUuid + ": " + e.getMessage());
            return null;
        }
    }

    public void deleteSession(UUID playerUuid) {
        File sessionFile = new File(storageDir, playerUuid + ".json");
        if (sessionFile.exists()) {
            sessionFile.delete();
            logger.debug("Deleted DoubleLife session file for " + playerUuid);
        }
    }

    private String[] serializeInventory(ItemStack[] items) {
        String[] serialized = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            serialized[i] = serializeItem(items[i]);
        }
        return serialized;
    }

    private ItemStack[] deserializeInventory(String[] data) {
        if (data == null) {
            return new ItemStack[0];
        }
        ItemStack[] items = new ItemStack[data.length];
        for (int i = 0; i < data.length; i++) {
            items[i] = deserializeItem(data[i]);
        }
        return items;
    }

    private String serializeItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.warn("Failed to serialize item for DoubleLife: " + e.getMessage());
            return null;
        }
    }

    private ItemStack deserializeItem(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            logger.warn("Failed to deserialize item for DoubleLife: " + e.getMessage());
            return null;
        }
    }

    private static class SessionData {
        String playerUuid;
        String playerName;
        long startTime;
        int duration;
        String[] activeProfiles;
        String[] savedInventory;
        String[] savedArmor;
        String[] savedEnderChest;
        String savedGameMode;
        LocationData savedLocation;
        double savedHealth;
        int savedFoodLevel;
        float savedExp;
        int savedLevel;
        String[] originalGroups;
        String temporaryGroupName;
    }

    private static class LocationData {
        String world;
        double x;
        double y;
        double z;
        float yaw;
        float pitch;

        static LocationData fromLocation(Location loc) {
            if (loc == null || loc.getWorld() == null) {
                return null;
            }
            LocationData data = new LocationData();
            data.world = loc.getWorld().getName();
            data.x = loc.getX();
            data.y = loc.getY();
            data.z = loc.getZ();
            data.yaw = loc.getYaw();
            data.pitch = loc.getPitch();
            return data;
        }

        Location toLocation() {
            return new Location(
                Bukkit.getWorld(world),
                x,
                y,
                z,
                yaw,
                pitch
            );
        }
    }
}
