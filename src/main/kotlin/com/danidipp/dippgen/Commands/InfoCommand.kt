package com.danidipp.dippgen.Commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class InfoCommand : ICommandImpl {
    override val name: String = "info"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        val target = if (args.isEmpty()) {
            return@CommandExecutor error(sender, "error: Specify a player")
        } else {
            Bukkit.getOfflinePlayerIfCached(args[0]) ?: return@CommandExecutor error(sender, "error: Player ${args[0]} does not exist")
        }

        sender.sendMessage(Component.text("This is a test command", NamedTextColor.GRAY))
        true
    }

    override val tabCompleter: TabCompleter = TabCompleter { sender, _, _, _ ->
        sender.server.offlinePlayers.mapNotNull { it.name }
    }

    private fun error(sender: CommandSender, message: String): Boolean {
        sender.sendMessage(Component.text(message, NamedTextColor.RED))
        return false
    }
}
