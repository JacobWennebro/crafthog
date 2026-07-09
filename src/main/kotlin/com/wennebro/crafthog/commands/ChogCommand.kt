package com.wennebro.crafthog.commands

import com.wennebro.crafthog.Crafthog
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

/**
 * Main command handler for /chog.
 *
 * Subcommands:
 *   reload — reloads the plugin configuration and re-registers modules.
 */
class ChogCommand(private val plugin: Crafthog) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            else -> showUsage(sender)
        }

        return true
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("crafthog.admin")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return
        }

        sender.sendMessage("${ChatColor.YELLOW}Reloading Crafthog...")
        plugin.reloadPlugin()
        sender.sendMessage("${ChatColor.GREEN}Crafthog reloaded successfully.")
    }

    private fun showUsage(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}=== Crafthog ===")
        sender.sendMessage("${ChatColor.GRAY}Captures this session: ${ChatColor.YELLOW}${plugin.getCaptureCount()}")
        sender.sendMessage("")
        sender.sendMessage("${ChatColor.YELLOW}/chog reload ${ChatColor.GRAY}- Reload plugin configuration")
    }
}
