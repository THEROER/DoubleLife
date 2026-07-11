# DoubleLife

A Bukkit/Paper plugin that gives staff a **second life**: a separate character
you switch into under a named profile, with its own inventory and elevated
permissions, then switch back out of. Sessions can be time-boxed, are announced
and logged to Discord, and the second life can be a throwaway loadout or a
persistent second character. Built on
[MagicUtils](https://github.com/THEROER/MagicUtils).

## How it works

- A player runs `/doublelife` and is matched to one or more **profiles** they're
  eligible for (e.g. `helper`, `moderator`, `admin`, `builder`, `developer`).
- For the profile's `duration`, they receive its permissions/group (integrates
  with **LuckPerms**) and see a boss-bar countdown. Their normal life is saved
  and hidden, and the second life is installed.
- When the timer runs out, or on `/doublelife` again / `/doublelife stop`, the
  session ends and the player's normal life is restored automatically.
- Start, end and in-session actions are posted to a Discord webhook.

### Second-life modes

Set per profile with `second-life-mode` (or globally under `second-life.mode`):

| Mode | On entry | On exit |
| --- | --- | --- |
| `EMPTY` | Empty inventory | Discarded |
| `KIT` | A fixed kit preset | Discarded |
| `PERSONA` | Your stored second character | **Saved** — carries over between sessions |

In `PERSONA` mode the second character is seeded from a kit the first time and
persists after that; unique items can't be duplicated because death keeps the
inventory instead of dropping it.

### What belongs to the second life

Inventory and armour always swap. Toggle the rest under `second-life.swap`:
ender chest, experience/levels, health/food. Hostile mobs can be prevented from
damaging or targeting second-life players (`second-life.mob-protection`).

## Kits

Moderators register named kit presets at runtime and reuse them as a second-life
seed or as always-give items.

| Command | Description |
| --- | --- |
| `/doublelife kit save <name>` | Save your current inventory as a kit. |
| `/doublelife kit give <name> [target]` | Give a kit to a player. |
| `/doublelife kit delete <name>` | Delete a kit. |
| `/doublelife kit list` | List saved kits. |

A profile's `seed-kit` is the starting contents of a KIT session or a PERSONA's
first-time seed; `always-give` kits are topped up on every entry if missing.

## Commands

Base command: `/doublelife` (alias `/dl`).

| Subcommand | Description |
| --- | --- |
| *(none)* | Toggle your own session on/off. |
| `start [target] [duration]` | Start a session. |
| `stop` | End a session (alias: `end`). |
| `info [target]` | Show session info. |
| `list` | List active sessions. |
| `kit …` | Manage kit presets (see above). |
| `settings` | Adjust your personal settings (e.g. mob protection). |
| `reload` | Reload the configuration. |

`/doublelife list`, `info`, `settings` and `kit list` render with hover
tooltips and clickable actions.

## Requirements

- A Bukkit/Paper server (API 1.21+, tested on 1.21.10; compatible up to 26.x).
- [MagicUtils](https://github.com/THEROER/MagicUtils) (shaded in).
- **LuckPerms** for group/permission management.

## Configuration

`plugins/DoubleLife/doublelife.yml`, highlights:

```yaml
enabled: true
storage-path: "doublelife"

second-life:
  mode: EMPTY                  # EMPTY | KIT | PERSONA (global default)
  storage: FILE                # FILE | SQLITE | MYSQL
  swap:
    inventory: true
    ender-chest: false
    experience: true
    health-food: true
  mob-protection:
    enabled: true
    player-adjustable: true    # players may override via /dl settings
  death-keeps-inventory: true
  database:                    # used only by the MYSQL backend
    host: localhost
    port: 3306
    database: doublelife
    username: root
    password: ""
    table-prefix: "dl_"

webhooks:
  enabled: true
  url: ""                      # Discord webhook URL
  action-log: true
  log-blocks: true
  log-inventory: true
  log-commands: true
  log-combat: true
  log-movement: true

profiles:
  helper:
    group-name: "helper"
    permissions:
      - "minecraft.command.teleport"
      - "essentials.vanish"
    duration: 1800             # 30 minutes; 0 = unlimited
    second-life-mode: PERSONA  # optional per-profile override
    seed-kit: helper-kit       # optional
    always-give: []            # optional
  moderator:
    # ...
```

Each profile can also run commands `before-start` / `after-start` /
`before-end` / `after-end`.

### Storage

Persona, kit and player-settings data is stored via the `second-life.storage`
backend: `FILE` (default, JSON files), `SQLITE` (embedded `doublelife.db`) or
`MYSQL` (also works with MariaDB, shared across servers). The plugin falls back
to files if the database can't be opened.

## Building

```bash
./gradlew build
```

## License

Licensed under the [Mozilla Public License 2.0](LICENSE).
