package dev.ua.theroer.doublelife.doublelife.storage;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Storage for named kit presets that moderators register at runtime and reuse
 * as a second-life seed or as always-give items. Backend-agnostic like
 * {@link PersonaStorage} so a database store can replace the local file store.
 */
public interface KitStorage {

    /** Loads a kit by name, or {@code null} if no kit with that name exists. */
    KitData load(String name);

    /** Saves (creating or overwriting) a kit under the given name. */
    void save(String name, KitData data);

    /** Removes a kit; returns whether one existed. */
    boolean delete(String name);

    /** Whether a kit with this name is stored. */
    boolean exists(String name);

    /** Names of all stored kits. */
    List<String> names();

    /** Contents of a kit preset. */
    final class KitData {
        public ItemStack[] inventory = new ItemStack[0];
        public ItemStack[] armor = new ItemStack[0];
    }
}
