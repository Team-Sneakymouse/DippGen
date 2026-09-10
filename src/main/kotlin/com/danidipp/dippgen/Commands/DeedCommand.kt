package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Modules.PlotManagement.Plot
import com.danidipp.dippgen.Modules.PlotManagement.PlotDeed
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class DeedCommand : ICommandImpl {
    override val name: String = "deed"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, _ ->
        val player = sender as? Player
        if (player == null) {
            sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val plot = Plot.getPlot(player.location)
        if (plot == null) {
            sender.sendMessage(Component.text("You are not in a plot", NamedTextColor.RED))
            return@CommandExecutor true
        }

        if (!plot.isManager(player) && !player.hasPermission("dipp.admin")) {
            sender.sendMessage(Component.text("You are not the owner of this plot", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val deedItem = PlotDeed.getDeedItem(plot, PlotDeed.DeedType.MANAGEMENT)
        if (deedItem == null) {
            sender.sendMessage(Component.text("Something went wrong. Please tell Dani! (${plot.id})", NamedTextColor.RED))
            return@CommandExecutor true
        }

        player.inventory.addItem(deedItem)
        true
    }

    override val tabCompleter: TabCompleter? = null
}
