package dev.ua.theroer.doublelife.config;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;

@Getter
public class WebhookSettings {
    @ConfigValue("enabled")
    @DefaultValue("true")
    @Comment("Enable Discord webhook notifications")
    private boolean enabled = true;

    @ConfigValue("url")
    @DefaultValue("")
    @Comment("Discord webhook URL")
    private String url = "";

    @ConfigValue("start-message")
    @DefaultValue("```fix\\nDoubleLife Started\\nPlayer: {player}\\nProfile: {profile}\\nDuration: {duration}\\n```")
    @Comment("Message sent when DoubleLife starts")
    private String startMessage = "```fix\\nDoubleLife Started\\nPlayer: {player}\\nProfile: {profile}\\nDuration: {duration}\\n```";

    @ConfigValue("end-message")
    @DefaultValue("```diff\\n- DoubleLife Ended\\nPlayer: {player}\\nProfile: {profile}\\n```")
    @Comment("Message sent when DoubleLife ends")
    private String endMessage = "```diff\\n- DoubleLife Ended\\nPlayer: {player}\\nProfile: {profile}\\n```";

    @ConfigValue("action-log")
    @DefaultValue("true")
    @Comment("Log player actions during DoubleLife")
    private boolean actionLog = true;

    @ConfigValue("action-message")
    @DefaultValue("```yaml\\nDoubleLife Action\\nPlayer: {player}\\nAction: {action}\\nDetails: {details}\\n```")
    @Comment("Message format for action logs")
    private String actionMessage = "```yaml\\nDoubleLife Action\\nPlayer: {player}\\nAction: {action}\\nDetails: {details}\\n```";

    @ConfigValue("action-batch-window-seconds")
    @Comment("Time window (seconds) to batch action logs before sending")
    private int actionBatchWindowSeconds = 2;

    @ConfigValue("action-batch-max-entries")
    @Comment("Max entries per batched action message; when reached, flush immediately")
    private int actionBatchMaxEntries = 25;

    @ConfigValue("action-batch-edit")
    @Comment("If true, reuse/edit the last Discord message for the batch to reduce spam")
    private boolean actionBatchEdit = true;
}
