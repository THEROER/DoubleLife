package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigFile;
import dev.ua.theroer.magicutils.config.annotations.ConfigReloadable;
import dev.ua.theroer.magicutils.config.annotations.ConfigSection;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;

import java.util.Map;

@Getter
@ConfigReloadable
@ConfigFile("doublelife.yml")
@Comment("DoubleLife configuration")
public class DoubleLifeConfig {

    @ConfigValue("enabled")
    @DefaultValue("true")
    @Comment("Enable DoubleLife system")
    private boolean enabled;

    @ConfigValue("storage-path")
    @DefaultValue("doublelife")
    @Comment("Relative path under plugin data folder for saved inventories")
    private String storagePath;

    @ConfigSection("webhooks")
    @Comment("Discord webhook settings")
    private WebhookSettings webhooks = new WebhookSettings();

    @ConfigSection("commands")
    @Comment("Console commands to execute on DoubleLife lifecycle")
    private CommandSettings commands = new CommandSettings();

    @ConfigValue("profiles")
    @Comment("Profiles mapped to LuckPerms groups")
    private Map<String, DoubleLifeProfile> profiles = DoubleLifeProfile.defaults();

    @ConfigValue("default-duration")
    @DefaultValue("1800")
    @Comment("Default duration in seconds (0 = unlimited)")
    private int defaultDuration;

    @ConfigValue("temporary-group")
    @DefaultValue("doublelife")
    @Comment("Base name for temporary groups created for DoubleLife")
    private String temporaryGroup;

    @ConfigValue("show-bossbar")
    @DefaultValue("true")
    @Comment("Show boss bar with remaining time")
    private boolean showBossBar;

    @ConfigValue("bossbar-color")
    @DefaultValue("GREEN")
    @Comment("Boss bar color")
    private String bossBarColor;

    @ConfigValue("bossbar-style")
    @DefaultValue("SOLID")
    @Comment("Boss bar style")
    private String bossBarStyle;

    public DoubleLifeConfig() {
        if (profiles == null) {
            profiles = DoubleLifeProfile.defaults();
        }
        if (commands == null) {
            commands = new CommandSettings();
        }
    }

    public void setProfiles(Map<String, DoubleLifeProfile> profiles) {
        this.profiles = profiles;
    }
}
