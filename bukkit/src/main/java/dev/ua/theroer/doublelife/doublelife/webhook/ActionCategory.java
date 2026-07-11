package dev.ua.theroer.doublelife.doublelife.webhook;

import dev.ua.theroer.doublelife.config.WebhookSettings;

import java.util.function.Predicate;

/**
 * Groups logged actions so each category can be toggled independently in config.
 */
public enum ActionCategory {
    BLOCKS(WebhookSettings::isLogBlocks),
    INVENTORY(WebhookSettings::isLogInventory),
    COMMANDS(WebhookSettings::isLogCommands),
    COMBAT(WebhookSettings::isLogCombat),
    MOVEMENT(WebhookSettings::isLogMovement);

    private final Predicate<WebhookSettings> enabled;

    ActionCategory(Predicate<WebhookSettings> enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled(WebhookSettings settings) {
        return enabled.test(settings);
    }
}
