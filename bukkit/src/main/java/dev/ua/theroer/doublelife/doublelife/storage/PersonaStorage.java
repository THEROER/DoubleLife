package dev.ua.theroer.doublelife.doublelife.storage;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Persistent storage for a player's second-life (PERSONA) character.
 *
 * <p>Kept separate from the per-session snapshot of the player's normal life:
 * a persona survives across DoubleLife sessions, a session snapshot does not.
 * The interface deliberately hides the backend so a network-backed
 * implementation (shared persona across servers) can replace the local file
 * store without touching the manager.
 */
public interface PersonaStorage {

    /**
     * Loads the stored persona for a player, or {@code null} if none exists yet
     * (the player has never built up a second-life character on this server).
     */
    PersonaData load(UUID playerUuid);

    /** Persists the player's current persona state. */
    void save(UUID playerUuid, PersonaData data);

    /** Removes a player's stored persona entirely. */
    void clear(UUID playerUuid);

    /** Whether a persona is stored for this player. */
    boolean exists(UUID playerUuid);

    /** Snapshot of the mutable state that belongs to a second-life character. */
    final class PersonaData {
        public ItemStack[] inventory = new ItemStack[0];
        public ItemStack[] armor = new ItemStack[0];
        public ItemStack[] enderChest = new ItemStack[0];
        public float exp;
        public int level;
        public double health = 20.0;
        public int foodLevel = 20;
    }
}
