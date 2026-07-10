package com.wennebro.crafthog.modules

import com.posthog.server.PostHogInterface
import com.wennebro.crafthog.config.ConfigManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerCommandPreprocessEvent

/**
 * Module that captures player command usage for analytics.
 * Tracks which commands are used and by whom.
 */
class CommandModule(
    private val posthog: PostHogInterface,
    private val serverVersion: String,
    private val config: ConfigManager
) : Module {

    override val id: String = "commands"

    override fun onEnable() {
        // Listener registration happens in the plugin class
    }

    override fun onDisable() {
        // Clean up if needed
    }

    private fun event(name: String): String {
        return "${config.eventsPrefix}_${name}"
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerCommand(event: PlayerCommandPreprocessEvent) {
        if (!config.isEventEnabled("ran_command")) return

        val player = event.player
        val command = event.message.split(" ").firstOrNull()?.removePrefix("/") ?: "unknown"
        val args = event.message.split(" ").drop(1)

        if (!config.settings.reportInvalidCommands && !isKnownCommand(command)) {
            return
        }

        val props = mapOf(
            "command" to command,
            "has_args" to args.isNotEmpty(),
            "arg_count" to args.size,
            "world" to player.world.name,
            "server_version" to serverVersion,
            "player_name" to player.name
        )

        posthog.capture(
            distinctId = player.uniqueId.toString(),
            event = event("ran_command"),
            properties = props
        )
    }

    /**
     * Checks whether a command name is registered in the server's command map.
     * Uses CraftBukkit internals via reflection; if that fails we conservatively
     * treat the command as valid so we don't drop real events.
     */
    private fun isKnownCommand(name: String): Boolean {
        return try {
            val server = Bukkit.getServer()
            val commandMap = server.javaClass
                .getMethod("getCommandMap")
                .invoke(server)
            val known = commandMap.javaClass
                .getMethod("getCommand", String::class.java)
                .invoke(commandMap, name)
            known != null
        } catch (_: Exception) {
            // Reflection failed (e.g. server impl changed) — be permissive
            true
        }
    }
}
