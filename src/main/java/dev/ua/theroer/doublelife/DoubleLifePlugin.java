package dev.ua.theroer.doublelife;

import dev.ua.theroer.doublelife.commands.DoubleLifeCommand;
import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeListener;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeManager;
import dev.ua.theroer.doublelife.doublelife.webhook.WebhookLifecycleNotifier;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.HelpCommand;
import dev.ua.theroer.magicutils.config.ConfigManager;
import dev.ua.theroer.magicutils.lang.LanguageManager;
import dev.ua.theroer.magicutils.lang.Messages;
import dev.ua.theroer.magicutils.platform.bukkit.BukkitPlatformProvider;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class DoubleLifePlugin extends JavaPlugin {

    @Getter
    private static DoubleLifePlugin instance;

    @Getter
    private ConfigManager configManager;
    @Getter
    private LanguageManager languageManager;
    @Getter
    private DoubleLifeConfig doubleLifeConfig;
    @Getter
    private DoubleLifeManager doubleLifeManager;
    @Getter
    private LuckPerms luckPerms;
    private WebhookLifecycleNotifier lifecycleNotifier;

    @Override
    public void onEnable() {
        instance = this;

        var platform = new BukkitPlatformProvider(this);
        configManager = new ConfigManager(platform);
        languageManager = new LanguageManager(platform, configManager);
        languageManager.init("en");
        Messages.setLanguageManager(languageManager);

        Logger.init(this, configManager);
        Logger.setLanguageManager(languageManager);
        Logger.setAutoLocalization(true);

        CommandRegistry.initialize(this, "doublelife");

        doubleLifeConfig = configManager.register(DoubleLifeConfig.class);
        registerLuckPerms();

        if (luckPerms == null) {
            Logger.error().send("LuckPerms not found - disabling DoubleLife");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        doubleLifeManager = new DoubleLifeManager(this, doubleLifeConfig, luckPerms);
        getServer().getPluginManager().registerEvents(new DoubleLifeListener(this), this);
        CommandRegistry.registerAll(
            new DoubleLifeCommand(this),
            new HelpCommand()
        );

        lifecycleNotifier = new WebhookLifecycleNotifier(doubleLifeManager.getWebhookManager(), doubleLifeConfig.getWebhooks());
        lifecycleNotifier.onEnable();

        Logger.info().send("DoubleLife plugin enabled");
    }

    @Override
    public void onDisable() {
        if (doubleLifeManager != null) {
            doubleLifeManager.shutdown();
        }
        if (lifecycleNotifier != null) {
            lifecycleNotifier.onDisable();
        }
        if (configManager != null) {
            configManager.shutdown();
        }
        Logger.info().send("DoubleLife disabled");
    }

    private void registerLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            luckPerms = null;
            return;
        }
        luckPerms = provider.getProvider();
        Logger.info().send("LuckPerms found - DoubleLife system enabled");
    }
}
