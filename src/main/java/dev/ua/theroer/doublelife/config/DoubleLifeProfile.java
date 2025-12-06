package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigSection;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

@ConfigSerializable
@Getter @Setter
public class DoubleLifeProfile {
    @ConfigValue("group-name")
    private String groupName;

    @ConfigValue("permissions")
    private List<String> permissions;

    @ConfigValue("allowed-commands")
    private List<String> allowedCommands;

    @ConfigSection("commands")
    private ProfileCommandSettings commands = new ProfileCommandSettings();

    @ConfigValue("duration")
    private int duration;

    public static Map<String, DoubleLifeProfile> defaults() {
        Map<String, DoubleLifeProfile> profiles = new HashMap<>();

        DoubleLifeProfile helper = new DoubleLifeProfile();
        helper.setGroupName("helper");
        helper.setPermissions(Arrays.asList(
            "commandwhitelist.group.doublelife-helper",
            "minecraft.command.teleport",
            "minecraft.command.msg",
            "essentials.fly",
            "essentials.vanish",
            "essentials.helpop.receive",
            "essentials.socialspy"
        ));
        helper.setDuration(1800);
        profiles.put("helper", helper);

        DoubleLifeProfile moderator = new DoubleLifeProfile();
        moderator.setGroupName("moderator");
        moderator.setPermissions(Arrays.asList(
            "commandwhitelist.group.doublelife-moderator",
            "minecraft.command.teleport",
            "minecraft.command.gamemode.spectator",
            "minecraft.command.kick",
            "minecraft.command.ban",
            "essentials.fly",
            "essentials.vanish",
            "essentials.god",
            "essentials.heal",
            "essentials.feed",
            "coreprotect.inspect",
            "worldedit.selection.*"
        ));
        moderator.setDuration(3600);
        profiles.put("moderator", moderator);

        DoubleLifeProfile admin = new DoubleLifeProfile();
        admin.setGroupName("admin");
        admin.setPermissions(Arrays.asList(
            "commandwhitelist.group.doublelife-admin",
            "minecraft.command.*",
            "essentials.*",
            "worldedit.*",
            "coreprotect.*",
            "luckperms.user.info",
            "luckperms.user.permission.check"
        ));
        admin.setDuration(7200);
        profiles.put("admin", admin);

        DoubleLifeProfile builder = new DoubleLifeProfile();
        builder.setGroupName("builder");
        builder.setPermissions(Arrays.asList(
            "commandwhitelist.group.doublelife-builder",
            "minecraft.command.teleport",
            "minecraft.command.gamemode.creative",
            "minecraft.command.gamemode.spectator",
            "minecraft.command.give",
            "minecraft.command.fill",
            "minecraft.command.clone",
            "minecraft.command.setblock",
            "worldedit.*",
            "voxelsniper.*",
            "essentials.fly",
            "essentials.speed",
            "essentials.god",
            "essentials.vanish"
        ));
        builder.setDuration(5400);
        profiles.put("builder", builder);

        DoubleLifeProfile developer = new DoubleLifeProfile();
        developer.setGroupName("developer");
        developer.setPermissions(Arrays.asList(
            "commandwhitelist.group.doublelife-developer",
            "*"
        ));
        developer.setDuration(0);
        profiles.put("developer", developer);

        return profiles;
    }
}
