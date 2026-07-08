# DoubleLife

A Bukkit/Paper plugin for **time-boxed elevated sessions** — a staff member
starts a "second life" under a named profile, temporarily gains that profile's
permissions/group for a set duration, then automatically reverts when it
expires. Every session is announced and logged to Discord via webhooks. Built
on [MagicUtils](https://github.com/THEROER/MagicUtils).

## How it works

- A player runs `/doublelife start` and is matched to one or more **profiles**
  they're eligible for (e.g. `helper`, `moderator`, `admin`, `builder`).
- For the profile's `duration`, they receive the configured permissions/group
  (integrates with **LuckPerms** when present) and see a boss-bar countdown.
- Their normal inventory is swapped out for the session and restored on exit,
  so a staff session doesn't touch their survival inventory.
- On start, end and configurable in-session actions, DoubleLife posts to a
  Discord webhook (with action batching to avoid spam).
- When the timer runs out — or on `/doublelife stop` — the session ends and the
  player is reverted automatically.

## Requirements

- A Bukkit/Paper server (API 1.20+).
- [MagicUtils](https://github.com/THEROER/MagicUtils).
- **LuckPerms** (soft dependency) for group/permission management.

## Commands

Base command: `/doublelife`.

| Subcommand | Description |
| --- | --- |
| `start` | Start a DoubleLife session. |
| `stop` | End your session (alias: `end`). |
| `info` | Show info about your current session. |
| `list` | List all active sessions. |
| `reload` | Reload the configuration. |

## Configuration

`plugins/DoubleLife/doublelife.yml` — highlights:

```yaml
enabled: true
storage-path: "doublelife"

webhooks:
  enabled: true
  url: ""                      # Discord webhook URL
  action-log: true
  action-batch-window-seconds: 2

# Named profiles: group, granted permissions and how long they last (seconds)
profiles:
  helper:
    group-name: "helper"
    permissions:
      - "minecraft.command.teleport"
      - "essentials.vanish"
    duration: 1800             # 30 minutes
  moderator:
    # ...
```

Each profile can also run commands `before-start` / `after-start` /
`before-end` / `after-end`.

## Building

```bash
./gradlew build
```

## License

Licensed under the [Mozilla Public License 2.0](LICENSE).
