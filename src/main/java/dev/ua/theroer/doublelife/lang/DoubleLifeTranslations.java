package dev.ua.theroer.doublelife.lang;

import dev.ua.theroer.magicutils.lang.LanguageManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registers built-in DoubleLife translations directly via MagicUtils.
 * Mirrors the RecipesTranslations approach to keep lang resources uncluttered.
 */
public final class DoubleLifeTranslations {

    private DoubleLifeTranslations() {
    }

    public static void register(LanguageManager languageManager) {
        if (languageManager == null) {
            return;
        }

        languageManager.saveCustomMessages("en", english());
        languageManager.saveCustomMessages("uk", ukrainian());
    }

    private static Map<String, String> english() {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("doublelife.enabled", "DoubleLife plugin enabled");
        map.put("doublelife.disabled", "DoubleLife plugin disabled");
        map.put("doublelife.luckperms.missing", "LuckPerms not found - disabling DoubleLife");
        map.put("doublelife.luckperms.found", "LuckPerms detected - DoubleLife hooks enabled");

        return map;
    }

    private static Map<String, String> ukrainian() {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("doublelife.enabled", "Плагін DoubleLife увімкнено");
        map.put("doublelife.disabled", "Плагін DoubleLife вимкнено");
        map.put("doublelife.luckperms.missing", "LuckPerms не знайдено — DoubleLife буде вимкнено");
        map.put("doublelife.luckperms.found", "LuckPerms знайдено — гачки DoubleLife активовано");

        return map;
    }
}
