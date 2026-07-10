package com.wennebro.crafthog.modules

import com.posthog.server.PostHogInterface
import com.wennebro.crafthog.config.ConfigManager
import org.bukkit.Bukkit

/**
 * Module that captures server-level events (plugin startup / shutdown).
 */
class ServerModule(
    private val posthog: PostHogInterface,
    private val serverVersion: String,
    private val config: ConfigManager,
    private val pluginVersion: String
) : Module {

    override val id: String = "server"

    override fun onEnable() {
        if (config.isEventEnabled("plugin_enabled")) {
            val prefix = config.eventsPrefix

            posthog.capture(
                distinctId = "server",
                event = "${prefix}_plugin_enabled",
                properties = mapOf(
                    "plugin_version" to pluginVersion,
                    "server_version" to serverVersion,
                    "online_players" to Bukkit.getOnlinePlayers().size,
                    "max_players" to Bukkit.getMaxPlayers(),
                    $$"$process_person_profile" to false
                )
            )
        }
    }

    override fun onDisable() {
        if (config.isEventEnabled("plugin_disabled")) {
            val prefix = config.eventsPrefix

            posthog.capture(
                distinctId = "server",
                event = "${prefix}_plugin_disabled",
                properties = mapOf(
                    "plugin_version" to pluginVersion,
                    "online_players" to Bukkit.getOnlinePlayers().size,
                    $$"$process_person_profile" to false
                )
            )
        }
    }
}
