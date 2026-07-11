package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

/**
 * Connection details for the MYSQL storage backend (also works with MariaDB).
 * Ignored by the FILE and SQLITE backends.
 */
@Getter
@Setter
@ConfigSerializable
public class DatabaseSettings {
    @ConfigValue("host")
    @DefaultValue("localhost")
    private String host = "localhost";

    @ConfigValue("port")
    @DefaultValue("3306")
    private int port = 3306;

    @ConfigValue("database")
    @DefaultValue("doublelife")
    private String database = "doublelife";

    @ConfigValue("username")
    @DefaultValue("root")
    private String username = "root";

    @ConfigValue("password")
    @DefaultValue("")
    private String password = "";

    @ConfigValue("table-prefix")
    @DefaultValue("dl_")
    @Comment("Prefix for DoubleLife tables, so several plugins can share one database")
    private String tablePrefix = "dl_";
}
