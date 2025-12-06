package dev.ua.theroer.doublelife.commands;

import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeManager;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeSession;
import dev.ua.theroer.magicutils.annotations.CommandInfo;
import dev.ua.theroer.magicutils.annotations.DefaultValue;
import dev.ua.theroer.magicutils.annotations.OptionalArgument;
import dev.ua.theroer.magicutils.annotations.Permission;
import dev.ua.theroer.magicutils.annotations.SubCommand;
import dev.ua.theroer.magicutils.commands.CommandResult;
import dev.ua.theroer.magicutils.commands.MagicCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandInfo(
    name = "doublelife",
    aliases = {"dl"},
    description = "Manage DoubleLife second life system"
)
@Permission("doublelife.command")
public class DoubleLifeCommand extends MagicCommand {

    private final DoubleLifePlugin plugin;
    private final DoubleLifeManager manager;

    public DoubleLifeCommand(DoubleLifePlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getDoubleLifeManager();
        if (this.manager == null) {
            throw new IllegalStateException("DoubleLifeManager is not initialized");
        }
    }

    public CommandResult execute(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players").color(NamedTextColor.RED));
            return CommandResult.failure("Players only");
        }

        if (manager.hasActiveSession(player.getUniqueId())) {
            manager.endDoubleLife(player);
            player.sendMessage(Component.text("DoubleLife mode disabled").color(NamedTextColor.RED));
        } else {
            var result = manager.startDoubleLife(player, 0);
            if (result.success()) {
                player.sendMessage(Component.text("DoubleLife mode enabled").color(NamedTextColor.GREEN));
            } else {
                return CommandResult.failure(result.reason());
            }
        }
        return CommandResult.success();
    }

    @SubCommand(name = "start", description = "Start DoubleLife mode", permission = "doublelife.command.start")
    public CommandResult start(
        @NotNull CommandSender sender,
        @OptionalArgument @DefaultValue("@sender") Player target,
        @OptionalArgument @DefaultValue("0") int duration
    ) {
        if (target == null && sender instanceof Player) {
            target = (Player) sender;
        } else if (target == null) {
            return CommandResult.failure("Console must specify a player target");
        }

        var result = manager.startDoubleLife(target, duration);
        if (result.success()) {
            return target.equals(sender)
                ? CommandResult.success()
                : CommandResult.success("DoubleLife started for " + target.getName());
        }
        return CommandResult.failure(result.reason());
    }

    @SubCommand(name = "end", description = "End DoubleLife mode", permission = "doublelife.command.end")
    public CommandResult end(
        @NotNull CommandSender sender,
        @OptionalArgument @DefaultValue("@sender") Player target
    ) {
        if (target == null && sender instanceof Player) {
            target = (Player) sender;
        } else if (target == null) {
            return CommandResult.failure("Console must specify a player target");
        }

        if (manager.endDoubleLife(target)) {
            if (!target.equals(sender)) {
                return CommandResult.success("DoubleLife ended for " + target.getName());
            }
            return CommandResult.success();
        } else {
            return CommandResult.failure(target.getName() + " doesn't have an active DoubleLife session");
        }
    }

    @SubCommand(name = "info", description = "View DoubleLife session info", permission = "doublelife.command.info")
    public CommandResult info(
        @NotNull CommandSender sender,
        @OptionalArgument @DefaultValue("@sender") Player target
    ) {
        if (target == null && sender instanceof Player) {
            target = (Player) sender;
        } else if (target == null) {
            return CommandResult.failure("Console must specify a player target");
        }

        DoubleLifeSession session = manager.getSession(target.getUniqueId());
        if (session == null) {
            return CommandResult.failure(target.getName() + " doesn't have an active DoubleLife session");
        }

        sender.sendMessage(Component.text("=== DoubleLife Info ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Player: ").color(NamedTextColor.YELLOW)
            .append(Component.text(target.getName()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Profiles: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.join(", ", session.getActiveProfiles())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Time Remaining: ").color(NamedTextColor.YELLOW)
            .append(Component.text(session.getFormattedRemainingTime()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Active: ").color(NamedTextColor.YELLOW)
            .append(Component.text(session.isActive() ? "Yes" : "No")
                .color(session.isActive() ? NamedTextColor.GREEN : NamedTextColor.RED)));

        return CommandResult.success();
    }

    @SubCommand(name = "list", description = "List all active DoubleLife sessions", permission = "doublelife.command.list")
    public CommandResult list(@NotNull CommandSender sender) {
        var sessions = manager.getActiveSessions();

        if (sessions.isEmpty()) {
            return CommandResult.failure("No active DoubleLife sessions");
        }

        sender.sendMessage(Component.text("=== Active DoubleLife Sessions ===").color(NamedTextColor.GOLD));
        for (DoubleLifeSession session : sessions) {
            sender.sendMessage(Component.text(session.getPlayerName()).color(NamedTextColor.YELLOW)
                .append(Component.text(" - ").color(NamedTextColor.GRAY))
                .append(Component.text(session.getFormattedRemainingTime()).color(NamedTextColor.WHITE))
                .append(Component.text(" - ").color(NamedTextColor.GRAY))
                .append(Component.text(String.join(", ", session.getActiveProfiles())).color(NamedTextColor.WHITE)));
        }

        return CommandResult.success();
    }

    @SubCommand(name = "reload", description = "Reload DoubleLife configuration", permission = "doublelife.command.reload")
    public CommandResult reload(@NotNull CommandSender sender) {
        plugin.getConfigManager().reload(DoubleLifeConfig.class);
        return CommandResult.success("DoubleLife configuration reloaded");
    }
}
