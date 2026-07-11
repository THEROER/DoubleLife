package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

/**
 * Controls whether players in their second life are protected from hostile mobs.
 * When {@code playerAdjustable} is true a player may override the default for
 * themselves via {@code /dl settings}.
 */
@Getter
@Setter
@ConfigSerializable
public class MobProtectionSettings {
    @ConfigValue("enabled")
    @DefaultValue("true")
    @Comment("Cancel damage to and hostile targeting of players in their second life")
    private boolean enabled = true;

    @ConfigValue("player-adjustable")
    @DefaultValue("true")
    @Comment("Let players override mob protection for themselves via /dl settings")
    private boolean playerAdjustable = true;
}
