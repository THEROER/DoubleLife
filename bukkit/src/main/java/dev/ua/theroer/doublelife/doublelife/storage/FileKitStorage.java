package dev.ua.theroer.doublelife.doublelife.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.magicutils.Logger;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Local file-backed {@link KitStorage}: one JSON file per kit under
 * {@code <dataFolder>/<storagePath>/kits/}.
 */
public class FileKitStorage implements KitStorage {

    private final File kitDir;
    private final Gson gson;
    private final Logger logger;
    private final ItemSerialization items;

    public FileKitStorage(DoubleLifePlugin plugin, String storagePath, ItemSerialization items) {
        this.kitDir = new File(new File(plugin.getDataFolder(), storagePath), "kits");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.items = items;
        this.logger = plugin.getMLogger();
        if (!kitDir.exists()) {
            kitDir.mkdirs();
        }
    }

    @Override
    public KitData load(String name) {
        File file = kitFile(name);
        if (file == null || !file.exists()) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath())) {
            StoredKit stored = gson.fromJson(reader, StoredKit.class);
            if (stored == null) {
                return null;
            }
            KitData data = new KitData();
            data.inventory = items.deserializeInventory(stored.inventory);
            data.armor = items.deserializeInventory(stored.armor);
            return data;
        } catch (Exception e) {
            logger.error("Failed to load kit '" + name + "': " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(String name, KitData data) {
        File file = kitFile(name);
        if (file == null) {
            return;
        }
        try {
            StoredKit stored = new StoredKit();
            stored.inventory = items.serializeInventory(data.inventory);
            stored.armor = items.serializeInventory(data.armor);
            try (Writer writer = Files.newBufferedWriter(file.toPath())) {
                gson.toJson(stored, writer);
            }
        } catch (Exception e) {
            logger.error("Failed to save kit '" + name + "': " + e.getMessage());
        }
    }

    @Override
    public boolean delete(String name) {
        File file = kitFile(name);
        if (file == null || !file.exists()) {
            return false;
        }
        return file.delete();
    }

    @Override
    public boolean exists(String name) {
        File file = kitFile(name);
        return file != null && file.exists();
    }

    @Override
    public List<String> names() {
        List<String> result = new ArrayList<>();
        File[] files = kitDir.listFiles((dir, n) -> n.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                String n = f.getName();
                result.add(n.substring(0, n.length() - ".json".length()));
            }
        }
        return result;
    }

    /**
     * Maps a kit name to its file, rejecting names that could escape the kits
     * directory or aren't a safe file name.
     */
    private File kitFile(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase();
        if (normalized.isEmpty() || !normalized.matches("[a-z0-9_-]{1,32}")) {
            return null;
        }
        return new File(kitDir, normalized + ".json");
    }

    private static class StoredKit {
        String[] inventory;
        String[] armor;
    }
}
