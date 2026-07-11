package dev.ua.theroer.doublelife.config;

/**
 * Where persistent second-life (PERSONA) data is stored. FILE is local and
 * needs no driver; SQLITE is a local embedded database; MYSQL is a shared
 * server database (also usable for MariaDB) that can back multiple servers.
 */
public enum StorageBackend {
    FILE,
    SQLITE,
    MYSQL
}
