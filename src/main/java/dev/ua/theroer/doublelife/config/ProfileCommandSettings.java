package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProfileCommandSettings {
    @ConfigValue("before-start")
    @Comment("Commands run before this profile starts (console). Placeholders: {player},{uuid},{profiles},{duration},{remaining}")
    private List<String> beforeStart = new ArrayList<>();

    @ConfigValue("after-start")
    private List<String> afterStart = new ArrayList<>();

    @ConfigValue("before-end")
    private List<String> beforeEnd = new ArrayList<>();

    @ConfigValue("after-end")
    private List<String> afterEnd = new ArrayList<>();
}
