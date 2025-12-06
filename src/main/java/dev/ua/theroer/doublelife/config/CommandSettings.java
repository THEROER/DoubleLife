package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandSettings {
    @ConfigValue("before-start")
    @Comment("Commands to run before DoubleLife starts (console). Placeholders: {player}, {uuid}, {profiles}, {duration}, {remaining}")
    private List<String> beforeStart = new ArrayList<>();

    @ConfigValue("after-start")
    @Comment("Commands to run after DoubleLife starts (console)")
    private List<String> afterStart = new ArrayList<>();

    @ConfigValue("before-end")
    @Comment("Commands to run before DoubleLife ends (console)")
    private List<String> beforeEnd = new ArrayList<>();

    @ConfigValue("after-end")
    @Comment("Commands to run after DoubleLife ends (console)")
    private List<String> afterEnd = new ArrayList<>();

    public void setBeforeStart(List<String> beforeStart) {
        this.beforeStart = beforeStart != null ? beforeStart : new ArrayList<>();
    }

    public void setAfterStart(List<String> afterStart) {
        this.afterStart = afterStart != null ? afterStart : new ArrayList<>();
    }

    public void setBeforeEnd(List<String> beforeEnd) {
        this.beforeEnd = beforeEnd != null ? beforeEnd : new ArrayList<>();
    }

    public void setAfterEnd(List<String> afterEnd) {
        this.afterEnd = afterEnd != null ? afterEnd : new ArrayList<>();
    }
}
