package dev.ua.theroer.doublelife;

import dev.ua.theroer.doublelife.commands.DoubleLifeCommand;
import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeListener;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeManager;
import dev.ua.theroer.doublelife.doublelife.webhook.WebhookLifecycleNotifier;
import dev.ua.theroer.doublelife.lang.DoubleLifeTranslations;
import dev.ua.theroer.magicutils.HelpCommand;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.HelpCommandSupport;
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
    private Logger mLogger;
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
        DoubleLifeTranslations.register(languageManager);
        Messages.setLanguageManager(languageManager);

        mLogger = new Logger(platform, this, configManager);
        mLogger.setLanguageManager(languageManager);
        mLogger.setAutoLocalization(true);

        CommandRegistry.initialize(this, "doublelife", mLogger);

        doubleLifeConfig = configManager.register(DoubleLifeConfig.class);
        registerLuckPerms();

        if (luckPerms == null) {
            mLogger.error("@doublelife.luckperms.missing");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        doubleLifeManager = new DoubleLifeManager(this, doubleLifeConfig, luckPerms);
        getServer().getPluginManager().registerEvents(new DoubleLifeListener(this), this);
        CommandRegistry.registerAll(
                new DoubleLifeCommand(this).addSubCommand(
                    HelpCommandSupport.createHelpSubCommand("help",
                        mLogger.getCore(), CommandRegistry::getCommandManager)));

        lifecycleNotifier = new WebhookLifecycleNotifier(doubleLifeManager.getWebhookManager(),
                doubleLifeConfig.getWebhooks());
        lifecycleNotifier.onEnable();

        mLogger.info("@doublelife.enabled");
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
        if (mLogger != null) {
            mLogger.info("@doublelife.disabled");
        }
    }

    private void registerLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager()
                .getRegistration(LuckPerms.class);
        if (provider == null) {
            luckPerms = null;
            return;
        }
        luckPerms = provider.getProvider();
        if (mLogger != null) {
            mLogger.info("@doublelife.luckperms.found");
        }
    }
}
