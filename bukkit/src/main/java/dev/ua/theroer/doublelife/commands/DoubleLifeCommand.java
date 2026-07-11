package dev.ua.theroer.doublelife.commands;

import dev.ua.theroer.doublelife.DoubleLifePlugin;
import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeManager;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeSession;
import dev.ua.theroer.doublelife.doublelife.storage.KitStorage;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.logger.MessageParser;
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

        String safe = MessageParser.escapeAttribute(target.getName());
        logger.info().noPrefix().to(sender).send(
            "<gray>=== <white>DoubleLife Info</white> ===</gray>\n"
                + "<gray>Player: <white>" + target.getName() + "</white>\n"
                + "<gray>Profiles: <white>" + String.join(", ", session.getActiveProfiles()) + "</white>\n"
                + "<gray>Time Remaining: <white>" + session.getFormattedRemainingTime() + "</white>\n"
                + "<gray>Active: <white>" + (session.isActive() ? "Yes" : "No") + "</white>\n"
                + "<dark_gray>[<red><hover:show_text:'End this session'>"
                + "<click:run_command:'/dl stop " + safe + "'>End DoubleLife</click></hover></red>]</dark_gray>"
        );

        return CommandResult.success();
    }

    @SubCommand(name = "list", description = "List all active DoubleLife sessions")
    public CommandResult list(@NotNull CommandSender sender) {
        var sessions = manager.getActiveSessions();

        if (sessions.isEmpty()) {
            return CommandResult.failure("No active DoubleLife sessions", false);
        }

        StringBuilder sb = new StringBuilder("<gray>=== <white>Active DoubleLife Sessions</white> ===</gray>");
        for (DoubleLifeSession session : sessions) {
            String name = session.getPlayerName();
            String safe = MessageParser.escapeAttribute(name);
            String profiles = MessageParser.escapeAttribute(String.join(", ", session.getActiveProfiles()));
            sb.append("\n")
                .append("<hover:show_text:'<gray>Profiles: <white>").append(profiles).append("</white><newline>")
                .append("<gray>Remaining: <white>").append(session.getFormattedRemainingTime()).append("</white><newline>")
                .append("<yellow>Click for full info'>")
                .append("<click:run_command:'/dl info ").append(safe).append("'>")
                .append("<aqua>").append(name).append("</aqua></click></hover> ")
                .append("<gray>-</gray> <white>").append(session.getFormattedRemainingTime()).append("</white> ")
                .append("<dark_gray>[<red><hover:show_text:'End this session'>")
                .append("<click:run_command:'/dl stop ").append(safe).append("'>stop</click></hover></red>]</dark_gray>");
        }
        logger.info().noPrefix().to(sender).send(sb.toString());

        return CommandResult.success();
    }

    @SubCommand(name = "reload", description = "Reload DoubleLife configuration")
    public CommandResult reload(@NotNull CommandSender sender) {
        plugin.getConfigManager().reload(DoubleLifeConfig.class);
        return CommandResult.success("DoubleLife configuration reloaded");
    }

    @SubCommand(name = "save", path = {"kit"}, description = "Save your current inventory as a named kit preset")
    public CommandResult kitSave(@Sender Player sender, @NotNull String name) {
        KitStorage.KitData kit = new KitStorage.KitData();
        kit.inventory = sender.getInventory().getContents().clone();
        kit.armor = sender.getInventory().getArmorContents().clone();
        manager.getKitStorage().save(name, kit);
        return CommandResult.success("Saved kit '" + name + "'");
    }

    @SubCommand(name = "give", path = {"kit"}, description = "Give a saved kit to a player")
    public CommandResult kitGive(
        @NotNull CommandSender sender,
        @NotNull String name,
        @DefaultValue("@sender") Player target
    ) {
        KitStorage.KitData kit = manager.getKitStorage().load(name);
        if (kit == null) {
            return CommandResult.failure("No kit named '" + name + "'", false);
        }
        for (org.bukkit.inventory.ItemStack item : kit.inventory) {
            if (item != null) {
                target.getInventory().addItem(item.clone());
            }
        }
        for (org.bukkit.inventory.ItemStack item : kit.armor) {
            if (item != null) {
                target.getInventory().addItem(item.clone());
            }
        }
        return CommandResult.success("Gave kit '" + name + "' to " + target.getName(), false);
    }

    @SubCommand(name = "delete", path = {"kit"}, aliases = {"remove"}, description = "Delete a saved kit")
    public CommandResult kitDelete(@NotNull CommandSender sender, @NotNull String name) {
        if (manager.getKitStorage().delete(name)) {
            return CommandResult.success("Deleted kit '" + name + "'");
        }
        return CommandResult.failure("No kit named '" + name + "'", false);
    }

    @SubCommand(name = "list", path = {"kit"}, description = "List saved kits")
    public CommandResult kitList(@NotNull CommandSender sender) {
        var names = manager.getKitStorage().names();
        if (names.isEmpty()) {
            return CommandResult.failure("No kits saved", false);
        }
        StringBuilder sb = new StringBuilder("<gray>=== <white>DoubleLife Kits</white> ===</gray>");
        for (String name : names) {
            String safe = MessageParser.escapeAttribute(name);
            sb.append("\n")
                .append("<hover:show_text:'<gray>Click to give <white>").append(safe).append("</white> to yourself'>")
                .append("<click:run_command:'/dl kit give ").append(safe).append("'>")
                .append("<aqua>").append(name).append("</aqua>")
                .append("</click></hover> ")
                .append("<dark_gray>[<red><hover:show_text:'Delete this kit'>")
                .append("<click:suggest_command:'/dl kit delete ").append(safe).append("'>x</click></hover></red>]</dark_gray>");
        }
        logger.info().noPrefix().to(sender).send(sb.toString());
        return CommandResult.success();
    }
}
