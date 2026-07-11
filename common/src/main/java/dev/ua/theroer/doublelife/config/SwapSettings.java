package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

/**
 * Which parts of a player's state belong to the second life and are swapped
 * when entering/leaving DoubleLife. Inventory and armour always swap together.
 */
@Getter
@Setter
@ConfigSerializable
public class SwapSettings {
    @ConfigValue("inventory")
    @DefaultValue("true")
    @Comment("Swap main inventory and armour (base of the mechanic)")
    private boolean inventory = true;

    @ConfigValue("ender-chest")
    @DefaultValue("false")
    @Comment("Give the second life its own persistent ender chest")
    private boolean enderChest = false;

    @ConfigValue("experience")
    @DefaultValue("true")
    @Comment("Give the second life its own experience and levels")
    private boolean experience = true;

    @ConfigValue("health-food")
    @DefaultValue("true")
    @Comment("Give the second life its own health and hunger")
    private boolean healthFood = true;
}
