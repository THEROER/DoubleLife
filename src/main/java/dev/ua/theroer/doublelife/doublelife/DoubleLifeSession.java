package dev.ua.theroer.doublelife.doublelife;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class DoubleLifeSession {
    private final UUID playerUuid;
    private final String playerName;
    private final Instant startTime;
    private int duration; // seconds
    private final Set<String> activeProfiles;

    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;
    private ItemStack[] savedEnderChest;
    private GameMode savedGameMode;
    private Location savedLocation;
    private double savedHealth;
    private int savedFoodLevel;
    private float savedExp;
    private int savedLevel;
    private List<String> originalGroups;

    private boolean active;
    private Instant endTime;
    private BukkitTask endTask;
    private String temporaryGroupName;

    public DoubleLifeSession(UUID playerUuid, String playerName, int duration, Set<String> activeProfiles) {
        this(playerUuid, playerName, Instant.now(), duration, activeProfiles);
    }

    public DoubleLifeSession(UUID playerUuid, String playerName, Instant startTime, int duration, Set<String> activeProfiles) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.startTime = startTime;
        this.duration = duration;
        this.activeProfiles = activeProfiles;
        this.active = true;
        this.originalGroups = new ArrayList<>();
    }

    public boolean isExpired() {
        if (duration == 0) {
            return false;
        }
        return Instant.now().isAfter(startTime.plusSeconds(duration));
    }

    public long getRemainingSeconds() {
        long elapsed = Instant.now().getEpochSecond() - startTime.getEpochSecond();
        return Math.max(0, duration - elapsed);
    }

    public String getFormattedRemainingTime() {
        long seconds = getRemainingSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        }
        return String.format("%02d:%02d", minutes, secs);
    }

    public void end() {
        this.active = false;
        this.endTime = Instant.now();
        cancel();
    }

    public void cancel() {
        if (endTask != null && !endTask.isCancelled()) {
            endTask.cancel();
            endTask = null;
        }
    }

    public void scheduleEnd(Runnable endAction, JavaPlugin plugin) {
        cancel();
        if (duration > 0) {
            long remainingSeconds = getRemainingSeconds();
            if (remainingSeconds > 0) {
                endTask = Bukkit.getScheduler().runTaskLater(plugin, endAction, remainingSeconds * 20L);
            }
        }
    }
}
