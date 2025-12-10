package dev.ua.theroer.doublelife.doublelife;

import dev.ua.theroer.doublelife.DoubleLifePlugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.block.Action;

public class DoubleLifeListener implements Listener {

    private final DoubleLifeManager manager;

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
        manager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            String command = event.getMessage().trim();
            manager.logAction(player, "Command", command);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            String from = formatLocation(event.getFrom());
            String to = formatLocation(event.getTo());
            manager.logAction(player, "Teleport", from + " -> " + to);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            manager.logAction(player, "GameMode", event.getNewGameMode().name());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            String block = event.getBlock().getType().name();
            String loc = formatLocation(event.getBlock().getLocation());
            manager.logAction(player, "BlockBreak", block + " at " + loc);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (manager.getSession(player.getUniqueId()) != null) {
            String block = event.getBlock().getType().name();
            String loc = formatLocation(event.getBlock().getLocation());
            manager.logAction(player, "BlockPlace", block + " at " + loc);
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
            manager.logAction(player, "Interact", action.name() + " " + block + " at " + loc);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (manager.getSession(player.getUniqueId()) != null) {
                String entity = event.getEntity().getType().name();
                manager.logAction(player, "Attack", entity + " (" + event.getFinalDamage() + " damage)");
            }
        }
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
