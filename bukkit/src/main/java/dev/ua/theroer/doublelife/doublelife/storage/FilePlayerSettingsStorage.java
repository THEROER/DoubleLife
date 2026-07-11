package dev.ua.theroer.doublelife.doublelife.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.doublelife.config.PlayerSetting;
import dev.ua.theroer.magicutils.Logger;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local file-backed {@link PlayerSettingsStorage}: one JSON file per player
 * under {@code <dataFolder>/<storagePath>/settings/}. Entries are cached in
 * memory and written through on change.
 */
public class FilePlayerSettingsStorage implements PlayerSettingsStorage {

    private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();

    private final File settingsDir;
    private final Gson gson;
    private final Logger logger;
    private final Map<UUID, Map<PlayerSetting, Boolean>> cache = new ConcurrentHashMap<>();

    public FilePlayerSettingsStorage(DoubleLifePlugin plugin, String storagePath) {
        this.settingsDir = new File(new File(plugin.getDataFolder(), storagePath), "settings");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.logger = plugin.getMLogger();
        if (!settingsDir.exists()) {
            settingsDir.mkdirs();
        }
    }

    @Override
    public Boolean get(UUID playerUuid, PlayerSetting setting) {
        return load(playerUuid).get(setting);
    }

    @Override
    public void set(UUID playerUuid, PlayerSetting setting, Boolean value) {
        Map<PlayerSetting, Boolean> map = load(playerUuid);
        if (value == null) {
            map.remove(setting);
        } else {
            map.put(setting, value);
        }
        save(playerUuid, map);
    }

    private Map<PlayerSetting, Boolean> load(UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, uuid -> {
            Map<PlayerSetting, Boolean> map = new EnumMap<>(PlayerSetting.class);
            File file = file(uuid);
            if (!file.exists()) {
                return map;
            }
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                Map<String, Boolean> raw = gson.fromJson(reader, MAP_TYPE);
                if (raw != null) {
                    for (Map.Entry<String, Boolean> entry : raw.entrySet()) {
                        try {
                            map.put(PlayerSetting.valueOf(entry.getKey()), entry.getValue());
                        } catch (IllegalArgumentException ignored) {
                            // Unknown setting key from an older/newer version; skip it.
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to load player settings for " + uuid + ": " + e.getMessage());
            }
            return map;
        });
    }

    private void save(UUID playerUuid, Map<PlayerSetting, Boolean> map) {
        File file = file(playerUuid);
        try {
            Map<String, Boolean> raw = new java.util.HashMap<>();
            for (Map.Entry<PlayerSetting, Boolean> entry : map.entrySet()) {
                raw.put(entry.getKey().name(), entry.getValue());
            }
            try (Writer writer = Files.newBufferedWriter(file.toPath())) {
                gson.toJson(raw, writer);
            }
        } catch (Exception e) {
            logger.error("Failed to save player settings for " + playerUuid + ": " + e.getMessage());
        }
    }

    private File file(UUID playerUuid) {
        return new File(settingsDir, playerUuid + ".json");
    }
}
