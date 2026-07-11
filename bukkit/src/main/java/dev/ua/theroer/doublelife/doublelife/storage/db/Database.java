package dev.ua.theroer.doublelife.doublelife.storage.db;

import dev.ua.theroer.doublelife.config.DatabaseSettings;
import dev.ua.theroer.doublelife.config.StorageBackend;
import dev.ua.theroer.magicutils.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns a single JDBC connection to either an embedded SQLite file or a
 * MySQL/MariaDB server and creates the DoubleLife schema. Kept deliberately
 * small: one connection guarded by a lock, enough for the plugin's low-traffic
 * persona/kit reads and writes.
 */
public final class Database {

    private final StorageBackend backend;
    private final DatabaseSettings settings;
    private final Logger logger;
    private final String prefix;
    private final File dataFolder;
    private final Object lock = new Object();
    private Connection connection;

    public Database(StorageBackend backend, DatabaseSettings settings, File dataFolder, Logger logger) {
        this.backend = backend;
        this.settings = settings;
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.prefix = backend == StorageBackend.MYSQL ? settings.getTablePrefix() : "";
    }

    public String table(String name) {
        return prefix + name;
    }

    /** Runs work with the shared connection, opening/reopening it as needed. */
    public <T> T withConnection(ConnectionFunction<T> work) throws SQLException {
        synchronized (lock) {
            return work.apply(connection());
        }
    }

    public void open() throws SQLException {
        synchronized (lock) {
            connection();
            createSchema();
        }
    }

    public void close() {
        synchronized (lock) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    logger.warn("Failed to close DoubleLife database: " + e.getMessage());
                }
                connection = null;
            }
        }
    }

    private Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            ensureDriver();
            connection = DriverManager.getConnection(url(), user(), password());
        }
        return connection;
    }

    /**
     * Loads the driver explicitly, since auto-registration via the service file
     * may not be honoured under Paper's isolated classloader. SQLite keeps its
     * original package (its JNI native library requires it); MariaDB is shaded
     * and relocated.
     */
    private void ensureDriver() throws SQLException {
        String driverClass = backend == StorageBackend.SQLITE
            ? "org.sqlite.JDBC"
            : "dev.ua.theroer.doublelife.libs.mariadb.Driver";
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found: " + driverClass, e);
        }
    }

    private String url() {
        if (backend == StorageBackend.SQLITE) {
            File dbFile = new File(dataFolder, "doublelife.db");
            return "jdbc:sqlite:" + dbFile.getAbsolutePath();
        }
        return "jdbc:mariadb://" + settings.getHost() + ":" + settings.getPort() + "/" + settings.getDatabase();
    }

    private String user() {
        return backend == StorageBackend.SQLITE ? null : settings.getUsername();
    }

    private String password() {
        return backend == StorageBackend.SQLITE ? null : settings.getPassword();
    }

    /** BLOB column type differs between the two dialects. */
    private String blobType() {
        return backend == StorageBackend.SQLITE ? "BLOB" : "MEDIUMBLOB";
    }

    private void createSchema() throws SQLException {
        String blob = blobType();
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + table("personas") + " ("
                + "uuid VARCHAR(36) PRIMARY KEY, "
                + "inventory " + blob + ", "
                + "armor " + blob + ", "
                + "ender_chest " + blob + ", "
                + "exp REAL, "
                + "level INTEGER, "
                + "health REAL, "
                + "food_level INTEGER)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + table("kits") + " ("
                + "name VARCHAR(32) PRIMARY KEY, "
                + "inventory " + blob + ", "
                + "armor " + blob + ")");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS " + table("player_settings") + " ("
                + "uuid VARCHAR(36), "
                + "setting VARCHAR(64), "
                + "value INTEGER, "
                + "PRIMARY KEY (uuid, setting))");
        }
    }

    @FunctionalInterface
    public interface ConnectionFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}
