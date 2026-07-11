package dev.ua.theroer.doublelife.doublelife.storage.db;

import dev.ua.theroer.doublelife.config.PlayerSetting;
import dev.ua.theroer.doublelife.doublelife.storage.PlayerSettingsStorage;
import dev.ua.theroer.magicutils.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/** Database-backed {@link PlayerSettingsStorage}. */
public class JdbcPlayerSettingsStorage implements PlayerSettingsStorage {

    private final Database db;
    private final Logger logger;

    public JdbcPlayerSettingsStorage(Database db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    @Override
    public Boolean get(UUID playerUuid, PlayerSetting setting) {
        try {
            return db.withConnection(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT value FROM " + db.table("player_settings")
                        + " WHERE uuid = ? AND setting = ?")) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, setting.name());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return null;
                        }
                        return rs.getInt("value") != 0;
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Failed to load setting for " + playerUuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void set(UUID playerUuid, PlayerSetting setting, Boolean value) {
        try {
            db.withConnection(conn -> {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM " + db.table("player_settings")
                        + " WHERE uuid = ? AND setting = ?")) {
                    del.setString(1, playerUuid.toString());
                    del.setString(2, setting.name());
                    del.executeUpdate();
                }
                if (value != null) {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO " + db.table("player_settings")
                            + " (uuid, setting, value) VALUES (?, ?, ?)")) {
                        ins.setString(1, playerUuid.toString());
                        ins.setString(2, setting.name());
                        ins.setInt(3, value ? 1 : 0);
                        ins.executeUpdate();
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to save setting for " + playerUuid + ": " + e.getMessage());
        }
    }
}
