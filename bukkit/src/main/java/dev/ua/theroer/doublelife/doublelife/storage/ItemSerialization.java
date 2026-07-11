package dev.ua.theroer.doublelife.doublelife.storage;

import dev.ua.theroer.magicutils.Logger;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;

/**
 * Base64 (de)serialization of ItemStacks, shared by the session snapshot store
 * and the persona store so the encoding lives in exactly one place.
 */
public final class ItemSerialization {

    private final Logger logger;

    public ItemSerialization(Logger logger) {
        this.logger = logger;
    }

    public String[] serializeInventory(ItemStack[] items) {
        if (items == null) {
            return new String[0];
        }
        String[] serialized = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            serialized[i] = serializeItem(items[i]);
        }
        return serialized;
    }

    public ItemStack[] deserializeInventory(String[] data) {
        if (data == null) {
            return new ItemStack[0];
        }
        ItemStack[] items = new ItemStack[data.length];
        for (int i = 0; i < data.length; i++) {
            items[i] = deserializeItem(data[i]);
        }
        return items;
    }

    public String serializeItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        try {
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.warn("Failed to serialize item for DoubleLife: " + e.getMessage());
            return null;
        }
    }

    public ItemStack deserializeItem(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            logger.warn("Failed to deserialize item for DoubleLife: " + e.getMessage());
            return null;
        }
    }
}
