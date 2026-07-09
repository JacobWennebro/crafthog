package com.wennebro.crafthog.modules

import com.posthog.server.PostHogInterface
import org.bukkit.configuration.ConfigurationSection
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
    moduleConfig: ConfigurationSection?,
    private val eventsPrefix: String
) : Module {

    override val id: String = "world"

    /** Block place tracking — enabled + type filter list */
    private val blockPlace = readTypedFilter(moduleConfig, "block_place")

    /** Block break tracking — enabled + type filter list */
    private val blockBreak = readTypedFilter(moduleConfig, "block_break")

    override fun onEnable() {
        // Listener registration happens in the plugin class
    }

    override fun onDisable() {
        // Clean up if needed
    }

    private fun event(name: String): String {
        return "${eventsPrefix}_${name}"
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!blockPlace.enabled) return

        val material = event.block.type
        if (!shouldCapture(material.name, blockPlace.types)) return

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
        if (!blockBreak.enabled) return

        val material = event.block.type
        if (!shouldCapture(material.name, blockBreak.types)) return

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

    private fun readTypedFilter(parent: ConfigurationSection?, key: String): TypedFilter {
        val section = parent?.getConfigurationSection(key)
        return TypedFilter(
            enabled = section?.getBoolean("enabled", false) ?: false,
            types = section?.getStringList("types") ?: emptyList()
        )
    }

    private data class TypedFilter(
        val enabled: Boolean,
        val types: List<String>
    )
}
