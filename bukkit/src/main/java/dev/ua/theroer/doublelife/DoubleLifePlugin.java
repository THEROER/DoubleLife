package dev.ua.theroer.doublelife;

import dev.ua.theroer.doublelife.commands.DoubleLifeCommand;
import dev.ua.theroer.doublelife.config.DoubleLifeConfig;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeListener;
import dev.ua.theroer.doublelife.doublelife.DoubleLifeManager;
import dev.ua.theroer.doublelife.doublelife.webhook.WebhookLifecycleNotifier;
import dev.ua.theroer.doublelife.lang.DoubleLifeTranslations;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.bootstrap.BukkitBootstrap;
import dev.ua.theroer.magicutils.bootstrap.MagicRuntime;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.HelpCommandSupport;
import dev.ua.theroer.magicutils.config.ConfigManager;
import dev.ua.theroer.magicutils.lang.LanguageManager;
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
    private CommandRegistry commandRegistry;
    private MagicRuntime runtime;
    private WebhookLifecycleNotifier lifecycleNotifier;

    @Override
    public void onEnable() {
        instance = this;

        BukkitBootstrap.RuntimeResult bootstrap = BukkitBootstrap.forPlugin(this)
                .permissionPrefix("doublelife")
                .translations(DoubleLifeTranslations::register)
                .enableCommands()
                .enableDiagnostics()
                .buildRuntime();
        runtime = bootstrap.runtime();
        configManager = bootstrap.configManager();
        languageManager = bootstrap.languageManager();
        mLogger = bootstrap.logger();
        commandRegistry = bootstrap.commandRegistry();

        doubleLifeConfig = configManager.register(DoubleLifeConfig.class);
        registerLuckPerms();

        if (luckPerms == null) {
            mLogger.error("@doublelife.luckperms.missing");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        doubleLifeManager = new DoubleLifeManager(this, doubleLifeConfig, luckPerms);
        getServer().getPluginManager().registerEvents(new DoubleLifeListener(this), this);

        DoubleLifeCommand command = new DoubleLifeCommand(this);
        command.addSubCommand(HelpCommandSupport.createHelpSubCommand(
                mLogger.getCore(), commandRegistry::commandManager));
        commandRegistry.registerAllCommands(command);

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
        if (mLogger != null) {
            mLogger.info("@doublelife.disabled");
        }
        if (runtime != null) {
            runtime.close();
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
