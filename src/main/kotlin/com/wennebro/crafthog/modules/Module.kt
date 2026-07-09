package com.wennebro.crafthog.modules

import org.bukkit.event.Listener

/**
 * Base interface for all analytics capture modules.
 * Each module can register event listeners and capture specific game events.
 */
interface Module : Listener {
    /**
     * Unique identifier for this module.
     */
    val id: String

    /**
     * Called when the module is enabled.
     */
    fun onEnable()

    /**
     * Called when the module is disabled.
     */
    fun onDisable()
}
