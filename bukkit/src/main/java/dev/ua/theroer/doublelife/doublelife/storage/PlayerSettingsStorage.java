package dev.ua.theroer.doublelife.doublelife.storage;

import dev.ua.theroer.doublelife.config.PlayerSetting;

import java.util.UUID;

/**
 * Per-player overrides for player-adjustable settings. A setting the player has
 * not explicitly chosen returns {@code null} and inherits the server default.
 * Backend-agnostic like the other stores.
 */
public interface PlayerSettingsStorage {

    /**
     * The player's explicit choice for a setting, or {@code null} to inherit the
     * server default.
     */
    Boolean get(UUID playerUuid, PlayerSetting setting);

    /** Sets (or with {@code null}, clears) a player's choice for a setting. */
    void set(UUID playerUuid, PlayerSetting setting, Boolean value);
}
