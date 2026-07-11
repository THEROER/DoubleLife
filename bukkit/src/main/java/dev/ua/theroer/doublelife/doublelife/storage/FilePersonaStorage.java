package dev.ua.theroer.doublelife.doublelife.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.magicutils.Logger;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Local file-backed {@link PersonaStorage}: one JSON file per player under
 * {@code <dataFolder>/<storagePath>/persona/}. A network-backed implementation
 * can replace this without touching the manager.
 */
public class FilePersonaStorage implements PersonaStorage {

    private final File personaDir;
    private final Gson gson;
    private final Logger logger;
    private final ItemSerialization items;

    public FilePersonaStorage(DoubleLifePlugin plugin, String storagePath, ItemSerialization items) {
        this.personaDir = new File(new File(plugin.getDataFolder(), storagePath), "persona");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.items = items;
        this.logger = plugin.getMLogger();
        if (!personaDir.exists()) {
            personaDir.mkdirs();
        }
    }

    @Override
    public PersonaData load(UUID playerUuid) {
        File file = personaFile(playerUuid);
        if (!file.exists()) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            StoredPersona stored = gson.fromJson(reader, StoredPersona.class);
            if (stored == null) {
                return null;
            }
            PersonaData data = new PersonaData();
            data.inventory = items.deserializeInventory(stored.inventory);
            data.armor = items.deserializeInventory(stored.armor);
            data.enderChest = items.deserializeInventory(stored.enderChest);
            data.exp = stored.exp;
            data.level = stored.level;
            data.health = stored.health;
            data.foodLevel = stored.foodLevel;
            return data;
        } catch (Exception e) {
            logger.error("Failed to load persona for " + playerUuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(UUID playerUuid, PersonaData data) {
        File file = personaFile(playerUuid);
        try {
            StoredPersona stored = new StoredPersona();
            stored.inventory = items.serializeInventory(data.inventory);
            stored.armor = items.serializeInventory(data.armor);
            stored.enderChest = items.serializeInventory(data.enderChest);
            stored.exp = data.exp;
            stored.level = data.level;
            stored.health = data.health;
            stored.foodLevel = data.foodLevel;
            try (Writer writer = Files.newBufferedWriter(file.toPath())) {
                gson.toJson(stored, writer);
            }
        } catch (Exception e) {
            logger.error("Failed to save persona for " + playerUuid + ": " + e.getMessage());
        }
    }

    @Override
    public void clear(UUID playerUuid) {
        File file = personaFile(playerUuid);
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public boolean exists(UUID playerUuid) {
        return personaFile(playerUuid).exists();
    }

    private File personaFile(UUID playerUuid) {
        return new File(personaDir, playerUuid + ".json");
    }

    private static class StoredPersona {
        String[] inventory;
        String[] armor;
        String[] enderChest;
        float exp;
        int level;
        double health;
        int foodLevel;
    }
}
