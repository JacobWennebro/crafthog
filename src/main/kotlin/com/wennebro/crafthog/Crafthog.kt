package com.wennebro.crafthog

import com.posthog.server.PostHog
import com.posthog.server.PostHogConfig
import com.posthog.server.PostHogInterface
import com.wennebro.crafthog.commands.ChogCommand
import com.wennebro.crafthog.config.ConfigManager
import com.wennebro.crafthog.modules.CommandModule
import com.wennebro.crafthog.modules.Module
import com.wennebro.crafthog.modules.PlayerModule
import com.wennebro.crafthog.modules.ServerModule
import com.wennebro.crafthog.modules.WorldModule
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicInteger
import java.lang.reflect.Proxy

class Crafthog : JavaPlugin() {

    private lateinit var configManager: ConfigManager
    private lateinit var realPosthog: PostHogInterface
    private lateinit var posthog: PostHogInterface
    private val modules = mutableListOf<Module>()
    private val captureCount = AtomicInteger(0)

    override fun onEnable() {
        configManager = ConfigManager(this)
        configManager.load()

        initPostHog()
        registerModules()
        registerCommands()

        val enabledEvents = configManager.getEnabledEvents()
        if (enabledEvents.isEmpty()) {
            logger.warning("No events are enabled. Uncomment events in config.yml to start capturing.")
        }

        logger.info("Crafthog enabled. PostHog host: ${configManager.posthog.host}")
        logger.info("Enabled events: ${if (enabledEvents.isEmpty()) "none" else enabledEvents.joinToString()}")
    }

    override fun onDisable() {
        // Let modules capture shutdown events (e.g. plugin_disabled) before flushing.
        modules.forEach { it.onDisable() }
        modules.clear()

        if (::posthog.isInitialized) {
            // Flush synchronously but with a timeout so the Paper watchdog doesn't kill us
            // if PostHog is unreachable. 15s is well under the default 60s watchdog threshold.
            val flushThread = Thread {
                try {
                    posthog.flush()
                } catch (e: Exception) {
                    logger.warning("PostHog flush failed: ${e.message}")
                }
            }
            flushThread.start()
            flushThread.join(15_000)
            if (flushThread.isAlive) {
                logger.warning("PostHog flush timed out after 15s. Some events may be lost.")
                flushThread.interrupt()
            }
        }

        logger.info("Crafthog disabled.")
    }

    private fun initPostHog() {
        if (configManager.posthog.apiKey.isBlank() || configManager.posthog.apiKey == "your-api-key-here") {
            logger.warning("PostHog API key is not configured! Please set it in config.yml and reload.")
            logger.warning("Plugin will start but no events will be captured.")
        }

        val phConfig = PostHogConfig(
            configManager.posthog.apiKey,
            configManager.posthog.host,
            configManager.posthog.debug,
        )
        // Explicitly set flush options due to Kotlin version mismatch with posthog-server library
        phConfig.flushAt = 20
        phConfig.flushIntervalSeconds = 30
        realPosthog = PostHog.with(phConfig)

        posthog = Proxy.newProxyInstance(
            PostHogInterface::class.java.classLoader,
            arrayOf(PostHogInterface::class.java)
        ) { _, method, args ->
            if (method.name == "capture") {
                captureCount.incrementAndGet()
            }
            method.invoke(realPosthog, *(args ?: emptyArray()))
        } as PostHogInterface
    }

    private fun registerModules() {
        val serverVersion = Bukkit.getBukkitVersion()

        // Server module (startup / shutdown events)
        val serverModule = ServerModule(posthog, serverVersion, configManager, description.version)
        serverModule.onEnable()
        modules.add(serverModule)

        // Commands module
        val commandModule = CommandModule(posthog, serverVersion, configManager)
        commandModule.onEnable()
        server.pluginManager.registerEvents(commandModule, this)
        modules.add(commandModule)

        // Players module
        val playerModule = PlayerModule(posthog, serverVersion, configManager, this)
        playerModule.onEnable()
        server.pluginManager.registerEvents(playerModule, this)
        modules.add(playerModule)

        // World module
        val worldModule = WorldModule(posthog, serverVersion, configManager)
        worldModule.onEnable()
        server.pluginManager.registerEvents(worldModule, this)
        modules.add(worldModule)
    }

    private fun registerCommands() {
        getCommand("chog")?.setExecutor(ChogCommand(this))
    }

    fun getCaptureCount(): Int = captureCount.get()

    fun reloadPlugin() {
        logger.info("Reloading Crafthog configuration...")
        configManager.reload()

        // Re-create PostHog client so api-key / host / debug changes take effect
        initPostHog()

        // Re-register modules so event toggles and settings take effect
        modules.forEach { it.onDisable() }
        modules.clear()
        registerModules()

        val enabledEvents = configManager.getEnabledEvents()
        if (enabledEvents.isEmpty()) {
            logger.warning("No events are enabled. Uncomment events in config.yml to start capturing.")
        }

        logger.info("Crafthog reloaded. Enabled events: ${if (enabledEvents.isEmpty()) "none" else enabledEvents.joinToString()}")
    }
}
