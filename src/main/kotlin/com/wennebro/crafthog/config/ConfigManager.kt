package com.wennebro.crafthog.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

/**
 * Handles loading and providing access to plugin configuration.
 * Events are now controlled by a flat list in config.yml.
 */
class ConfigManager(private val plugin: JavaPlugin) {

    data class PostHogConfig(
        val apiKey: String,
        val host: String,
        val debug: Boolean
    )

    data class Settings(
        val reportInvalidCommands: Boolean,
        val commandTypes: List<String>,
        val foodConsumedTypes: List<String>,
        val blockPlaceTypes: List<String>,
        val blockBreakTypes: List<String>
    )

    lateinit var posthog: PostHogConfig
        private set

    lateinit var settings: Settings
        private set

    lateinit var eventsPrefix: String
        private set

    var identifyPlayers: Boolean = true
        private set

    private val enabledEvents = mutableSetOf<String>()

    fun load() {
        plugin.saveDefaultConfig()
        val config: FileConfiguration = plugin.config

        posthog = PostHogConfig(
            apiKey = config.getString("posthog.api-key", "")!!,
            host = config.getString("posthog.host", "https://eu.i.posthog.com")!!,
            debug = config.getBoolean("posthog.debug", false)
        )

        eventsPrefix = config.getString("events_prefix", "mc")!!
        identifyPlayers = config.getBoolean("identify_players", true)

        enabledEvents.clear()
        val eventList = config.getStringList("events")
        enabledEvents.addAll(eventList.map { it.lowercase() })

        settings = Settings(
            reportInvalidCommands = config.getBoolean("settings.report_invalid_commands", true),
            commandTypes = config.getStringList("settings.command_types"),
            foodConsumedTypes = config.getStringList("settings.food_consumed_types"),
            blockPlaceTypes = config.getStringList("settings.block_place_types"),
            blockBreakTypes = config.getStringList("settings.block_break_types")
        )
    }

    /**
     * Check if a specific event is enabled in the config.
     */
    fun isEventEnabled(name: String): Boolean {
        return enabledEvents.contains(name.lowercase())
    }

    fun getEnabledEvents(): Set<String> = enabledEvents.toSet()

    fun reload() {
        plugin.reloadConfig()
        load()
    }
}
