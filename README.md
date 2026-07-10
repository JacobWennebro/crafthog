# CraftHog

Event tracking and analytics for Minecraft servers, powered by [PostHog](https://posthog.com).

Capture player activity, world interactions, and command usage on your Minecraft server and send it straight to PostHog for analysis, dashboards, and insights.

---

## What it does

- **Per-event capture** - enable only the events you need by uncommenting them
- **Custom event prefixes** - namespace your events (like `mc_player_joined`, `mc_block_placed`)
- **Typed filtering** - track only specific blocks, foods, or commands
- **Player identification** - identify players in PostHog with custom properties
- **Personless events** - plugin events do not create PostHog person profiles
- **Live session counter** - see how many events have been captured this session
- **Zero-downtime reload** - reload config and events without restarting

---

## Events

| Event | Category | Description |
|-------|----------|-------------|
| `plugin_enabled` | Server | Fired when the plugin starts up |
| `plugin_disabled` | Server | Fired when the plugin shuts down |
| `player_joined` | Player | Fired when a player joins the server |
| `player_left` | Player | Fired when a player leaves the server |
| `player_died` | Player | Fired when a player dies |
| `player_kill` | Player | Fired when a player kills another player |
| `player_jumped` | Player | Fired when a player jumps (very noisy) |
| `player_sneaked` | Player | Fired when a player starts sneaking (moderately noisy) |
| `player_ate` | Player | Fired when a player eats food (moderately noisy) |
| `ran_command` | Command | Fired when a player runs a command |
| `block_placed` | World | Fired when a player places a block (very noisy) |
| `block_broken` | World | Fired when a player breaks a block (very noisy) |

All events are prefixed with your configured `events_prefix` (default: `mc`).

Missing a data point you want to track? [Open an issue](../../issues) and let me know.

---

## Installation

1. Download the latest JAR from [Releases](../../releases)
2. Drop it into your server's `plugins/` folder
3. Restart your server
4. Edit `plugins/Crafthog/config.yml` with your PostHog API key
5. Uncomment the events you want to capture
6. Run `/chog reload` or restart

---

## Configuration

```yaml
# Prefix for all PostHog events
events_prefix: mc

posthog:
  api-key: "phc_your_project_api_key"
  host: "https://eu.i.posthog.com"
  debug: false

# Identify players in PostHog when they join (recommended).
identify_players: true

events:
  # Uncomment the events you want to capture.
  # - plugin_enabled
  # - plugin_disabled
  # - player_joined
  # - player_left
  # - player_died
  # - player_kill
  # - player_jumped    # Very noisy
  # - player_sneaked     # Moderately noisy
  # - player_ate         # Moderately noisy without type filtering
  # - ran_command
  # - block_placed       # Very noisy without type filtering
  # - block_broken       # Very noisy without type filtering

settings:
  # Applies to: ran_command
  report_invalid_commands: true

  # Applies to: player_ate
  food_consumed_types:
    - GOLDEN_APPLE
    - ENCHANTED_GOLDEN_APPLE

  # Applies to: block_placed
  block_place_types:
    - BEDROCK

  # Applies to: block_broken
  block_break_types:
    - BEDROCK
```

### Typed Filters

Use the `types` list under any tracked item to whitelist specific values. Case-insensitive.

```yaml
settings:
  food_consumed_types:
    - GOLDEN_APPLE
    - ENCHANTED_GOLDEN_APPLE
```

---

## Commands and Permissions

| Command | Permission | Description |
|---------|------------|-------------|
| `/chog` | `crafthog.admin` | Shows session stats and available commands |
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
