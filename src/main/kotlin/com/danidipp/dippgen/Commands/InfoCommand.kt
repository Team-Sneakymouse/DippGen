package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Modules.PlotManagement.Plot
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class InfoCommand : ICommandImpl {
    override val name: String = "info"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        val target = if (args.isEmpty()) {
            val player = sender as? Player ?: return@CommandExecutor error(
                sender,
                "error: You must be on the server to use this command without specifying a player",
            )

            val plot = Plot.getPlot(player.location) ?: return@CommandExecutor error(sender, "error: You are not on a plot")
            val owner = plot.region.owners.uniqueIds.firstOrNull() ?: return@CommandExecutor error(sender, "error: Plot has no owner")
            Bukkit.getOfflinePlayer(owner)
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
