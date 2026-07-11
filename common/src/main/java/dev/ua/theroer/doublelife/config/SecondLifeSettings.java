package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSection;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

/**
 * Global defaults for the second-life inventory behaviour. Profiles may
 * override the mode; the swap area applies globally unless a profile narrows it.
 */
@Getter
@Setter
@ConfigSerializable
public class SecondLifeSettings {
    @ConfigValue("mode")
    @DefaultValue("EMPTY")
    @Comment("Default second-inventory mode: EMPTY | KIT | PERSONA")
    private SecondLifeMode mode = SecondLifeMode.EMPTY;

    @ConfigValue("storage")
    @DefaultValue("FILE")
    @Comment("Where PERSONA data is stored: FILE | SQLITE | MYSQL")
    private StorageBackend storage = StorageBackend.FILE;

    @ConfigSection("swap")
    @Comment("Which parts of player state belong to the second life")
    private SwapSettings swap = new SwapSettings();
}
