package dev.ua.theroer.doublelife.doublelife;

import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.doublelife.doublelife.webhook.ActionCategory;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DoubleLifeListener implements Listener {

    private final DoubleLifeManager manager;

    /** Snapshot of a container's contents taken on open, keyed by the viewing player. */
    private final Map<UUID, Map<Material, Integer>> openContainerSnapshots = new ConcurrentHashMap<>();

    public DoubleLifeListener(DoubleLifePlugin plugin) {
        this.manager = plugin.getDoubleLifeManager();
        if (this.manager == null) {
            throw new IllegalStateException("DoubleLifeManager is not initialized");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.handlePlayerJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        openContainerSnapshots.remove(event.getPlayer().getUniqueId());
        manager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            String command = event.getMessage().trim();
            manager.logAction(player, ActionCategory.COMMANDS, "Command", command);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            String from = formatLocation(event.getFrom());
            String to = formatLocation(event.getTo());
            manager.logAction(player, ActionCategory.MOVEMENT, "Teleport", from + " -> " + to);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            manager.logAction(player, ActionCategory.MOVEMENT, "GameMode", event.getNewGameMode().name());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            String block = event.getBlock().getType().name();
            String loc = formatLocation(event.getBlock().getLocation());
            manager.logAction(player, ActionCategory.BLOCKS, "BlockBreak", block + " at " + loc);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            String block = event.getBlock().getType().name();
            String loc = formatLocation(event.getBlock().getLocation());
            manager.logAction(player, ActionCategory.BLOCKS, "BlockPlace", block + " at " + loc);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null && event.getClickedBlock() != null) {
            Action action = event.getAction();
            if (action == Action.LEFT_CLICK_BLOCK) {
                return;
            }
            if (action == Action.RIGHT_CLICK_BLOCK) {
                // If player is holding a block and placement is allowed, let BlockPlaceEvent handle it
                if (event.getItem() != null
                    && event.getItem().getType().isBlock()
                    && event.useItemInHand() != Event.Result.DENY) {
                    return;
                }
            }
            String block = event.getClickedBlock().getType().name();
            String loc = formatLocation(event.getClickedBlock().getLocation());
            manager.logAction(player, ActionCategory.BLOCKS, "Interact", action.name() + " " + block + " at " + loc);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (manager.getSession(player.getUniqueId()) != null) {
                String entity = event.getEntity().getType().name();
                manager.logAction(player, ActionCategory.COMBAT, "Attack", entity + " (" + event.getFinalDamage() + " damage)");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDamagePlayer(EntityDamageByEntityEvent event) {
        // Cancel damage dealt to a protected player by anything that isn't another player.
        if (event.getEntity() instanceof Player player
            && !(event.getDamager() instanceof Player)
            && manager.isMobProtected(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player player
            && manager.isMobProtected(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (manager.keepsInventoryOnDeath(player.getUniqueId())) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (manager.getSession(player.getUniqueId()) == null) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (!isTrackedContainer(inventory, player)) {
            return;
        }
        openContainerSnapshots.put(player.getUniqueId(), countContents(inventory));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Map<Material, Integer> before = openContainerSnapshots.remove(player.getUniqueId());
        if (before == null || manager.getSession(player.getUniqueId()) == null) {
            return;
        }
        Inventory inventory = event.getInventory();
        Map<Material, Integer> after = countContents(inventory);
        String delta = describeDelta(before, after);
        if (delta.isEmpty()) {
            return;
        }
        manager.logAction(player, ActionCategory.INVENTORY, "Container", containerLabel(inventory) + ": " + delta);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) == null) {
            return;
        }
        ItemStack stack = event.getItemDrop().getItemStack();
        manager.logAction(player, ActionCategory.INVENTORY, "Drop", describeStack(stack));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (manager.getSession(player.getUniqueId()) == null) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        manager.logAction(player, ActionCategory.INVENTORY, "Pickup", describeStack(stack));
    }

    /**
     * Tracks only external containers (chests, ender chests, barrels, shulkers, etc.),
     * not the player's own inventory or crafting/creative views.
     */
    private boolean isTrackedContainer(Inventory inventory, Player player) {
        if (inventory == null) {
            return false;
        }
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Player owner) {
            // The player's own inventory (or ender chest opened on self) is not a container transfer.
            return !owner.getUniqueId().equals(player.getUniqueId());
        }
        return holder != null;
    }

    private Map<Material, Integer> countContents(Inventory inventory) {
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            counts.merge(item.getType(), item.getAmount(), Integer::sum);
        }
        return counts;
    }

    /**
     * Builds a human-readable diff between two content snapshots, e.g. "+64 STONE, -1 DIAMOND".
     * Positive means items were deposited into the container, negative means withdrawn.
     */
    private String describeDelta(Map<Material, Integer> before, Map<Material, Integer> after) {
        Map<Material, Integer> diff = new LinkedHashMap<>();
        for (Map.Entry<Material, Integer> entry : after.entrySet()) {
            diff.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        for (Map.Entry<Material, Integer> entry : before.entrySet()) {
            diff.merge(entry.getKey(), -entry.getValue(), Integer::sum);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : diff.entrySet()) {
            int change = entry.getValue();
            if (change == 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(change > 0 ? "+" : "-")
                .append(Math.abs(change))
                .append(' ')
                .append(entry.getKey().name());
        }
        return sb.toString();
    }

    private String describeStack(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return "nothing";
        }
        return stack.getAmount() + "x " + stack.getType().name();
    }

    private String containerLabel(Inventory inventory) {
        String type = inventory.getType().name();
        Location loc = inventory.getLocation();
        if (loc != null) {
            return type + " at " + formatLocation(loc);
        }
        return type;
    }

    private String formatLocation(Location loc) {
        if (loc == null) {
            return "null";
        }
        return String.format(
            "%s:%d,%d,%d",
            loc.getWorld() != null ? loc.getWorld().getName() : "null",
            loc.getBlockX(),
            loc.getBlockY(),
            loc.getBlockZ()
        );
    }
}
