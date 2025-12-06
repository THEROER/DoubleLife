package dev.ua.theroer.doublelife.doublelife;

import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DoubleLifeBossBarManager {

    private final JavaPlugin plugin;
    private final DoubleLifeConfig config;
    private final Map<UUID, BossBar> activeBossBars;
    private final Map<UUID, BukkitTask> updateTasks;

    public DoubleLifeBossBarManager(JavaPlugin plugin, DoubleLifeConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.activeBossBars = new ConcurrentHashMap<>();
        this.updateTasks = new ConcurrentHashMap<>();
    }

    public void createBossBar(Player player, DoubleLifeSession session) {
        if (!config.isShowBossBar()) {
            return;
        }

        removeBossBar(player.getUniqueId());

        BarColor color = parseBarColor(config.getBossBarColor());
        BarStyle style = parseBarStyle(config.getBossBarStyle());

        BossBar bossBar = Bukkit.createBossBar(formatTitle(session), color, style);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        activeBossBars.put(player.getUniqueId(), bossBar);

        if (session.getDuration() > 0) {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline() || !session.isActive()) {
                    removeBossBar(player.getUniqueId());
                    return;
                }
                updateBossBar(player.getUniqueId(), session);
            }, 0L, 20L);

            updateTasks.put(player.getUniqueId(), task);
        }
    }

    private void updateBossBar(UUID playerUuid, DoubleLifeSession session) {
        BossBar bossBar = activeBossBars.get(playerUuid);
        if (bossBar == null) {
            return;
        }

        bossBar.setTitle(formatTitle(session));

        if (session.getDuration() > 0) {
            double progress = (double) session.getRemainingSeconds() / session.getDuration();
            bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

            if (progress < 0.1) {
                bossBar.setColor(BarColor.RED);
            } else if (progress < 0.25) {
                bossBar.setColor(BarColor.YELLOW);
            }
        } else {
            bossBar.setProgress(1.0);
        }
    }

    public void removeBossBar(UUID playerUuid) {
        BukkitTask task = updateTasks.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }

        BossBar bossBar = activeBossBars.remove(playerUuid);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void removeAllBossBars() {
        updateTasks.values().forEach(BukkitTask::cancel);
        updateTasks.clear();
        activeBossBars.values().forEach(BossBar::removeAll);
        activeBossBars.clear();
    }

    private String formatTitle(DoubleLifeSession session) {
        if (session.getDuration() == 0) {
            return "§aDoubleLife Mode §7- §eUnlimited";
        }
        String time = session.getFormattedRemainingTime();
        String profiles = String.join(", ", session.getActiveProfiles());
        return "§aDoubleLife §7[§f" + profiles + "§7] §7- §e" + time;
    }

    private BarColor parseBarColor(String colorName) {
        try {
            return BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }

    private BarStyle parseBarStyle(String styleName) {
        try {
            return BarStyle.valueOf(styleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }
}
