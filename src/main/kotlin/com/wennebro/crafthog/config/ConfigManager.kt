package com.wennebro.crafthog.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

/**
 * Handles loading and providing access to plugin configuration.
 */
class ConfigManager(private val plugin: JavaPlugin) {

    data class PostHogConfig(
        val apiKey: String,
        val host: String,
        val debug: Boolean
    )

    data class ModuleConfig(
        val commands: Boolean,
        val players: Boolean,
        val world: Boolean
    )

    lateinit var posthog: PostHogConfig
        private set

    lateinit var modules: ModuleConfig
        private set

    lateinit var eventsPrefix: String
        private set

    fun load() {
        plugin.saveDefaultConfig()
        val config: FileConfiguration = plugin.config

        posthog = PostHogConfig(
            apiKey = config.getString("posthog.api-key", "")!!,
            host = config.getString("posthog.host", "https://eu.i.posthog.com")!!,
            debug = config.getBoolean("posthog.debug", false)
        )

        eventsPrefix = config.getString("events_prefix", "mc")!!

        modules = ModuleConfig(
            commands = isModuleEnabled(config, "commands"),
            players = isModuleEnabled(config, "players"),
            world = isModuleEnabled(config, "world")
        )
    }

    /**
     * Returns the configuration section for a specific module.
     * Modules read their own toggles/fields from this section.
     */
    fun getModuleSection(id: String): ConfigurationSection? {
        return plugin.config.getConfigurationSection("modules.$id")
    }

    /**
     * Reads the master on/off toggle for a module.
     * Supports both the legacy flat boolean format (`commands: true`)
     * and the newer section format (`commands.enabled: true`).
     */
    private fun isModuleEnabled(config: FileConfiguration, id: String): Boolean {
        val path = "modules.$id"
        return when {
            config.isBoolean(path) -> config.getBoolean(path, true)
            config.isConfigurationSection(path) -> {
                config.getConfigurationSection(path)?.getBoolean("enabled", true) ?: true
            }
            else -> true
        }
    }

    fun reload() {
        plugin.reloadConfig()
        load()
    }
}
