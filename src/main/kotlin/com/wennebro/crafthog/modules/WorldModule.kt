package com.wennebro.crafthog.modules

import com.posthog.server.PostHogInterface
import com.wennebro.crafthog.config.ConfigManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

/**
 * Module that captures world interaction events (block place / break).
 */
class WorldModule(
    private val posthog: PostHogInterface,
    private val serverVersion: String,
    private val config: ConfigManager
) : Module {

    override val id: String = "world"

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
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!config.isEventEnabled("block_placed")) return

        val material = event.block.type
        if (!shouldCapture(material.name, config.settings.blockPlaceTypes)) return

        val player = event.player

        posthog.capture(
            distinctId = player.uniqueId.toString(),
            event = event("block_placed"),
            properties = buildMap {
                put("player_name", player.name)
                put("block_type", material.name)
                put("world", player.world.name)
                put("x", event.block.x)
                put("y", event.block.y)
                put("z", event.block.z)
                put("server_version", serverVersion)
            }
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!config.isEventEnabled("block_broken")) return

        val material = event.block.type
        if (!shouldCapture(material.name, config.settings.blockBreakTypes)) return

        val player = event.player

        posthog.capture(
            distinctId = player.uniqueId.toString(),
            event = event("block_broken"),
            properties = buildMap {
                put("player_name", player.name)
                put("block_type", material.name)
                put("world", player.world.name)
                put("x", event.block.x)
                put("y", event.block.y)
                put("z", event.block.z)
                put("server_version", serverVersion)
            }
        )
    }

    private fun shouldCapture(name: String, allowed: List<String>): Boolean {
        if (allowed.isEmpty()) return true
        return allowed.any { it.equals(name, ignoreCase = true) }
    }
}
