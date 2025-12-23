package dev.ua.theroer.doublelife.commands;

import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeManager;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeSession;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.annotations.CommandInfo;
import dev.ua.theroer.magicutils.annotations.DefaultValue;
import dev.ua.theroer.magicutils.annotations.Permission;
import dev.ua.theroer.magicutils.annotations.Sender;
import dev.ua.theroer.magicutils.annotations.SubCommand;
import dev.ua.theroer.magicutils.commands.CommandResult;
import dev.ua.theroer.magicutils.commands.CompareMode;
import dev.ua.theroer.magicutils.commands.MagicCommand;
import dev.ua.theroer.magicutils.commands.PermissionConditionType;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandInfo(
    name = "doublelife",
    aliases = {"dl"},
    description = "Manage DoubleLife second life system"
)
public class DoubleLifeCommand extends MagicCommand {

    private final DoubleLifePlugin plugin;
    private final DoubleLifeManager manager;
    private final Logger logger;

    public DoubleLifeCommand(DoubleLifePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getMLogger();
        this.manager = plugin.getDoubleLifeManager();
        if (this.manager == null) {
            throw new IllegalStateException("DoubleLifeManager is not initialized");
        }
    }

    public CommandResult execute(@Sender Player sender) {
        if (manager.hasActiveSession(sender.getUniqueId())) {
            manager.endDoubleLife(sender);
            return CommandResult.success();
        } else {
            var result = manager.startDoubleLife(sender, 0);
            if (result.success()) {
                return CommandResult.success();
            }
            return CommandResult.failure(false);
        }
    }

    @SubCommand(name = "start", description = "Start DoubleLife mode")
    public CommandResult start(
        @NotNull CommandSender sender,
        @DefaultValue("@sender")
        @Permission(node="other", condition = PermissionConditionType.OTHER, compare = CompareMode.UUID, conditionArgs = {"target"})
        Player target,
        @DefaultValue("0") int duration
    ) {
        var result = manager.startDoubleLife(target, duration);
        if (result.success()) {
            if (sender instanceof Player && sender.equals(target)) {
                return CommandResult.success();
            }
            return CommandResult.success("DoubleLife started for " + target.getName(), false);
        }
        if (!(sender instanceof Player && sender.equals(target))) {
            return CommandResult.failure(result.reason());
        }
        return CommandResult.failure(false);
    }

    @SubCommand(name = "stop", aliases = {"end"}, description = "End DoubleLife mode")
    public CommandResult end(
        @NotNull CommandSender sender,
        @DefaultValue("@sender") 
        @Permission(node="other", condition = PermissionConditionType.OTHER, compare = CompareMode.UUID, conditionArgs = {"target"})
        Player target
    ) {
        if (manager.endDoubleLife(target)) {
            if (!target.equals(sender)) {
                return CommandResult.success("DoubleLife ended for " + target.getName(), false);
            }
            return CommandResult.success();
        } else {
            return CommandResult.failure(target.getName() + " doesn't have an active DoubleLife session", false);
        }
    }

    @SubCommand(name = "info", description = "View DoubleLife session info")
    public CommandResult info(
        @NotNull CommandSender sender,
        @DefaultValue("@sender") 
        @Permission(node="other", condition = PermissionConditionType.OTHER, compare = CompareMode.UUID, conditionArgs = {"target"})
        Player target
    ) {
        DoubleLifeSession session = manager.getSession(target.getUniqueId());
        if (session == null) {
            return CommandResult.failure(target.getName() + " doesn't have an active DoubleLife session", false);
        }

        logger.info().to(sender).send("=== DoubleLife Info ===");
        logger.info().to(sender).send("Player: " + target.getName());
        logger.info().to(sender).send("Profiles: " + String.join(", ", session.getActiveProfiles()));
        logger.info().to(sender).send("Time Remaining: " + session.getFormattedRemainingTime());
        logger.info().to(sender).send("Active: " + (session.isActive() ? "Yes" : "No"));

        return CommandResult.success();
    }

    @SubCommand(name = "list", description = "List all active DoubleLife sessions")
    public CommandResult list(@NotNull CommandSender sender) {
        var sessions = manager.getActiveSessions();

        if (sessions.isEmpty()) {
            return CommandResult.failure("No active DoubleLife sessions", false);
        }

        logger.info().to(sender).send("=== Active DoubleLife Sessions ===");
        for (DoubleLifeSession session : sessions) {
            logger.info().to(sender).send(
                session.getPlayerName() + " - " + session.getFormattedRemainingTime()
                    + " - " + String.join(", ", session.getActiveProfiles())
            );
        }

        return CommandResult.success();
    }

    @SubCommand(name = "reload", description = "Reload DoubleLife configuration")
    public CommandResult reload(@NotNull CommandSender sender) {
        plugin.getConfigManager().reload(DoubleLifeConfig.class);
        return CommandResult.success("DoubleLife configuration reloaded");
    }
}
