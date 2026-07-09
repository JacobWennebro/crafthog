package com.wennebro.crafthog.modules

import com.posthog.server.PostHogInterface
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/**
 * Module that captures player lifecycle events (join, leave, death) and
 * identifies players in PostHog. All sub-features are individually toggleable.
 */
class PlayerModule(
    private val posthog: PostHogInterface,
    private val serverVersion: String,
    moduleConfig: ConfigurationSection?,
    private val plugin: JavaPlugin,
    private val eventsPrefix: String
) : Module {

    override val id: String = "players"

    private val captureJoin: Boolean = moduleConfig?.getBoolean("capture_join", true) ?: true
    private val captureLeave: Boolean = moduleConfig?.getBoolean("capture_leave", true) ?: true
    private val captureIdentify: Boolean = moduleConfig?.getBoolean("capture_identify", true) ?: true
    private val captureDeath: Boolean = moduleConfig?.getBoolean("capture_death", true) ?: true
    private val captureJump: Boolean = moduleConfig?.getBoolean("capture_jump", false) ?: false
    private val captureSneak: Boolean = moduleConfig?.getBoolean("capture_sneak", false) ?: false

    /** Food consumed tracking — enabled + type filter list */
    private val foodConsumed = readTypedFilter(moduleConfig, "food_consumed")

    /**
     * Tracks the last vertical delta per player. Used to detect the *start*
     * of a jump (first tick of upward motion) without relying on the
     * client-controlled isOnGround flag.
     */
    private val lastDy = mutableMapOf<UUID, Double>()

    /**
     * Stores pre-consume food / saturation values so we can compute the
     * delta on the next tick. Keyed by player UUID.
     */
    private val preConsumeStats = mutableMapOf<UUID, Pair<Int, Float>>()

    override fun onEnable() {
        // Listener registration happens in the plugin class
    }

    override fun onDisable() {
        lastDy.clear()
        preConsumeStats.clear()
    }

    private fun event(name: String): String {
        return "${eventsPrefix}_${name}"
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!captureJoin && !captureIdentify) return

        val player = event.player
        val id = player.uniqueId.toString()
        val ip = player.address?.address?.hostAddress

        lastDy[player.uniqueId] = 0.0

        if (captureIdentify) {
            posthog.identify(
                distinctId = id,
                userProperties = buildMap {
                    put("name", player.name)
                    put("first_join", !player.hasPlayedBefore())
                    if (ip != null) {
                        put("\$ip", ip)
                    }
                }
            )
        }

        if (captureJoin) {
            posthog.capture(
                distinctId = id,
                event = event("player_joined"),
                properties = buildMap {
                    put("player_name", player.name)
                    if (ip != null) {
                        put("ip_address", ip)
                    }
                    put("world", player.world.name)
                    put("server_version", serverVersion)
                    put("online_count", player.server.onlinePlayers.size)
                }
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (captureLeave) {
            val player = event.player
            val id = player.uniqueId.toString()

            posthog.capture(
                distinctId = id,
                event = event("player_left"),
                properties = buildMap {
                    put("player_name", player.name)
                    put("world", player.world.name)
                    put("server_version", serverVersion)
                    put("online_count", player.server.onlinePlayers.size - 1)
                }
            )
        }

        lastDy.remove(event.player.uniqueId)
        preConsumeStats.remove(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        if (!captureDeath) return

        val player = event.player
        val id = player.uniqueId.toString()
        val killer = event.player.killer

        posthog.capture(
            distinctId = id,
            event = event("player_died"),
            properties = buildMap {
                put("player_name", player.name)
                put("world", player.world.name)
                put("server_version", serverVersion)
                put("death_message", event.deathMessage() ?: "unknown")
                if (killer != null) {
                    put("killer_name", killer.name)
                    put("killer_uuid", killer.uniqueId.toString())
                    put("killer_is_player", true)
                } else {
                    put("killer_is_player", false)
                }
                put("drops", event.drops.size)
                put("keep_inventory", event.keepInventory)
                put("keep_level", event.keepLevel)
            }
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!captureJump) return

        val player = event.player
        if (player.isFlying || player.vehicle != null || player.isGliding) return

        val dy = event.to.y - event.from.y
        if (dy < 0.3) return

        val prevDy = lastDy[player.uniqueId] ?: 0.0
        if (prevDy > 0.01) {
            lastDy[player.uniqueId] = dy
            return
        }

        if (!wasOnGround(event.from)) {
            lastDy[player.uniqueId] = dy
            return
        }

        posthog.capture(
            distinctId = player.uniqueId.toString(),
            event = event("player_jumped"),
            properties = buildMap {
                put("player_name", player.name)
                put("world", player.world.name)
                put("server_version", serverVersion)
                put("is_sprinting", player.isSprinting)
            }
        )

        lastDy[player.uniqueId] = dy
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        if (!captureSneak) return
        if (!event.isSneaking) return

        val player = event.player

        posthog.capture(
            distinctId = player.uniqueId.toString(),
            event = event("player_sneaked"),
            properties = buildMap {
                put("player_name", player.name)
                put("world", player.world.name)
                put("server_version", serverVersion)
            }
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemConsume(event: PlayerItemConsumeEvent) {
        if (!foodConsumed.enabled) return

        val material = event.item.type
        if (!material.isEdible) return
        if (!shouldCapture(material.name, foodConsumed.types)) return

        val player = event.player
        val id = player.uniqueId

        // At MONITOR priority the caller hasn't resumed yet, so food/saturation
        // effects have NOT been applied. Record the pre-values now.
        val preFood = player.foodLevel
        val preSaturation = player.saturation
        preConsumeStats[id] = preFood to preSaturation

        // Read post-values on the next tick after the server has applied them.
        Bukkit.getScheduler().runTask(plugin) { _ ->
            val (preF, preS) = preConsumeStats.remove(id) ?: return@runTask
            if (!player.isOnline) return@runTask

            val postFood = player.foodLevel
            val postSaturation = player.saturation

            posthog.capture(
                distinctId = id.toString(),
                event = event("player_ate"),
                properties = buildMap {
                    put("player_name", player.name)
                    put("food_type", material.name)
                    put("world", player.world.name)
                    put("server_version", serverVersion)
                    put("pre_food", preF)
                    put("post_food", postFood)
                    put("food_delta", postFood - preF)
                    put("pre_saturation", preS)
                    put("post_saturation", postSaturation)
                    put("saturation_delta", postSaturation - preS)
                }
            )
        }
    }

    private fun wasOnGround(location: Location): Boolean {
        return location.clone().subtract(0.0, 0.01, 0.0).block.type.isSolid
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
