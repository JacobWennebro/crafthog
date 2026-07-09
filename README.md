# CraftHog

Event tracking and analytics for Minecraft servers, powered by [PostHog](https://posthog.com).

Capture player activity, world interactions, and command usage on your Minecraft server and send it straight to PostHog for analysis, dashboards, and insights.

---

## What it does

- **Modular event capture** - enable only what you need
- **Custom event prefixes** - namespace your events (like `mc_player_joined`, `mc_block_placed`)
- **Typed filtering** - track only specific blocks, foods, or commands
- **Player identification** - identify players in PostHog with custom properties
- **Personless events** - plugin events do not create PostHog person profiles
- **Live session counter** - see how many events have been captured this session
- **Zero-downtime reload** - reload config and modules without restarting

---

## Modules

| Module | Events | Description |
|--------|--------|-------------|
| **Players** | `player_joined`, `player_left`, `player_died`, `player_jumped`, `player_sneaked`, `food_consumed` | Lifecycle, movement, death, and food tracking |
| **World** | `block_placed`, `block_broken` | Block interaction analytics |
| **Commands** | `ran_command` | Command usage tracking |

All events are prefixed with your configured `events_prefix` (default: `mc`).

---

## Installation

1. Download the latest JAR from [Releases](../../releases)
2. Drop it into your server's `plugins/` folder
3. Restart your server
4. Edit `plugins/Crafthog/config.yml` with your PostHog API key
5. Run `/chog reload` or restart

---

## Configuration

```yaml
# Prefix for all PostHog events
events_prefix: mc

posthog:
  api-key: "phc_your_project_api_key"
  host: "https://eu.i.posthog.com"
  debug: false

modules:
  commands:
    enabled: true
    report_invalid_commands: true

  players:
    enabled: true
    capture_identify: true
    capture_join: true
    capture_leave: true
    capture_death: true
    capture_jump: false      # Very noisy
    capture_sneak: false     # Moderately noisy
    food_consumed:
      enabled: true
      types: []              # Empty = all food items

  world:
    enabled: true
    block_place:
      enabled: true
      types:
        - BEDROCK
    block_break:
      enabled: true
      types: []              # Empty = all blocks
```

### Typed Filters

Use the `types` list under any tracked item to whitelist specific values. Case-insensitive.

```yaml
food_consumed:
  enabled: true
  types:
    - GOLDEN_APPLE
    - ENCHANTED_GOLDEN_APPLE
```

---

## Commands and Permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/chog` | none | Shows session stats and available commands |
| `/chog reload` | `crafthog.admin` | Reloads configuration and re-registers all modules |

The `crafthog.admin` permission defaults to **op**.

---

## Building from Source

Requires JDK 26 or newer.

```bash
./gradlew build
```

The fat JAR will be at `build/libs/crafthog-<version>-all.jar`.

### Local test server

```bash
./gradlew runServer
```

Spins up a Paper 1.26.2 server with the plugin automatically loaded.

---

## Tech Stack

- [Kotlin](https://kotlinlang.org) - JVM language
- [Paper](https://papermc.io) - Minecraft server API
- [PostHog Server SDK](https://github.com/PostHog/posthog-java) - Event ingestion
- [Shadow Gradle Plugin](https://gradleup.com/shadow/) - Fat JAR packaging

---

## License

MIT
