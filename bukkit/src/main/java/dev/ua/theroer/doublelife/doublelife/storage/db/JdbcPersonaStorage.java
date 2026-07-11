package dev.ua.theroer.doublelife.doublelife.storage.db;

import dev.ua.theroer.doublelife.doublelife.storage.ItemSerialization;
import dev.ua.theroer.doublelife.doublelife.storage.PersonaStorage;
import dev.ua.theroer.magicutils.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/** Database-backed {@link PersonaStorage} (SQLite or MySQL via {@link Database}). */
public class JdbcPersonaStorage implements PersonaStorage {

    private final Database db;
    private final ItemSerialization items;
    private final Logger logger;

    public JdbcPersonaStorage(Database db, ItemSerialization items, Logger logger) {
        this.db = db;
        this.items = items;
        this.logger = logger;
    }

    @Override
    public PersonaData load(UUID playerUuid) {
        try {
            return db.withConnection(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM " + db.table("personas") + " WHERE uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return null;
                        }
                        PersonaData data = new PersonaData();
                        data.inventory = items.fromBlob(rs.getBytes("inventory"));
                        data.armor = items.fromBlob(rs.getBytes("armor"));
                        data.enderChest = items.fromBlob(rs.getBytes("ender_chest"));
                        data.exp = rs.getFloat("exp");
                        data.level = rs.getInt("level");
                        data.health = rs.getDouble("health");
                        data.foodLevel = rs.getInt("food_level");
                        return data;
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Failed to load persona for " + playerUuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(UUID playerUuid, PersonaData data) {
        try {
            db.withConnection(conn -> {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM " + db.table("personas") + " WHERE uuid = ?")) {
                    del.setString(1, playerUuid.toString());
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO " + db.table("personas")
                        + " (uuid, inventory, armor, ender_chest, exp, level, health, food_level)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ins.setString(1, playerUuid.toString());
                    ins.setBytes(2, items.toBlob(data.inventory));
                    ins.setBytes(3, items.toBlob(data.armor));
                    ins.setBytes(4, items.toBlob(data.enderChest));
                    ins.setFloat(5, data.exp);
                    ins.setInt(6, data.level);
                    ins.setDouble(7, data.health);
                    ins.setInt(8, data.foodLevel);
                    ins.executeUpdate();
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to save persona for " + playerUuid + ": " + e.getMessage());
        }
    }

    @Override
    public void clear(UUID playerUuid) {
        try {
            db.withConnection(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM " + db.table("personas") + " WHERE uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to clear persona for " + playerUuid + ": " + e.getMessage());
        }
    }

    @Override
    public boolean exists(UUID playerUuid) {
        try {
            return db.withConnection(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM " + db.table("personas") + " WHERE uuid = ?")) {
                    ps.setString(1, playerUuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Failed to check persona for " + playerUuid + ": " + e.getMessage());
            return false;
        }
    }
}
