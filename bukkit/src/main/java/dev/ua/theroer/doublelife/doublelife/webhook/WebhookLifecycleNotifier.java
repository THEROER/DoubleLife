package dev.ua.theroer.doublelife.doublelife.webhook;

import dev.ua.theroer.doublelife.config.WebhookSettings;
import org.bukkit.Bukkit;

public class WebhookLifecycleNotifier {
    private final WebhookManager webhookManager;
    private final WebhookSettings settings;

    public WebhookLifecycleNotifier(WebhookManager webhookManager, WebhookSettings settings) {
        this.webhookManager = webhookManager;
        this.settings = settings;
    }

    public void onEnable() {
        if (!canSend()) return;
        String msg = "**DoubleLife enabled** on server `" + Bukkit.getServer().getName() + "`";
        webhookManager.sendSimple(msg, 0x00FF00);
    }

    public void onDisable() {
        if (!canSend()) return;
        String msg = "**DoubleLife disabled** on server `" + Bukkit.getServer().getName() + "`";
        webhookManager.sendSimple(msg, 0xFF0000);
    }

    private boolean canSend() {
        return settings.isEnabled() && settings.getUrl() != null && !settings.getUrl().isEmpty();
    }
}
