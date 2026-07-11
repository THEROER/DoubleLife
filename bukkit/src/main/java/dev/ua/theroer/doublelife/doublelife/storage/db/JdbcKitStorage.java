package dev.ua.theroer.doublelife.doublelife.storage.db;

import dev.ua.theroer.doublelife.doublelife.storage.ItemSerialization;
import dev.ua.theroer.doublelife.doublelife.storage.KitStorage;
import dev.ua.theroer.magicutils.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Database-backed {@link KitStorage}. */
public class JdbcKitStorage implements KitStorage {

    private final Database db;
    private final ItemSerialization items;
    private final Logger logger;

    public JdbcKitStorage(Database db, ItemSerialization items, Logger logger) {
        this.db = db;
        this.items = items;
        this.logger = logger;
    }

    private String normalize(String name) {
        if (name == null) {
            return null;
        }
        String n = name.trim().toLowerCase();
        return n.matches("[a-z0-9_-]{1,32}") ? n : null;
    }

    @Override
    public KitData load(String name) {
        String key = normalize(name);
        if (key == null) {
            return null;
        }
        try {
            return db.withConnection(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT inventory, armor FROM " + db.table("kits") + " WHERE name = ?")) {
                    ps.setString(1, key);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return null;
                        }
                        KitData data = new KitData();
                        data.inventory = items.fromBlob(rs.getBytes("inventory"));
                        data.armor = items.fromBlob(rs.getBytes("armor"));
                        return data;
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Failed to load kit '" + name + "': " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(String name, KitData data) {
        String key = normalize(name);
        if (key == null) {
            return;
        }
        try {
            db.withConnection(conn -> {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM " + db.table("kits") + " WHERE name = ?")) {
                    del.setString(1, key);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO " + db.table("kits") + " (name, inventory, armor) VALUES (?, ?, ?)")) {
                    ins.setString(1, key);
                    ins.setBytes(2, items.toBlob(data.inventory));
                    ins.setBytes(3, items.toBlob(data.armor));
                    ins.executeUpdate();
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Failed to save kit '" + name + "': " + e.getMessage());
        }
    }

    @Override
    public boolean delete(String name) {
        String key = normalize(name);
        if (key == null) {
            return false;
        }
        try {
            return db.withConnection(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM " + db.table("kits") + " WHERE name = ?")) {
                    ps.setString(1, key);
                    return ps.executeUpdate() > 0;
                }
            });
        } catch (Exception e) {
            logger.error("Failed to delete kit '" + name + "': " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String name) {
        return load(name) != null;
    }

    @Override
    public List<String> names() {
        try {
            return db.withConnection(conn -> {
                List<String> result = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT name FROM " + db.table("kits") + " ORDER BY name")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            result.add(rs.getString("name"));
                        }
                    }
                }
                return result;
            });
        } catch (Exception e) {
            logger.error("Failed to list kits: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
