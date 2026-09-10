package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Modules.PlotManagement.Plot
import com.sk89q.worldedit.bukkit.BukkitAdapter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter

class HomestoneDestinationCommand : ICommandImpl {
    override val name: String = "homestonedestination"

    override val executor: CommandExecutor = CommandExecutor { _, _, _, args ->
        if (args.isEmpty()) {
            return@CommandExecutor false
        }

        val player = Bukkit.getPlayer(args[0]) ?: return@CommandExecutor false
        val plot = Plot.getPlot(player.location)
        if (plot != null && Plot.getOwnedPlots(player).contains(plot)) {
            plot.region.setFlag(Plot.teleportLocationFlag, BukkitAdapter.adapt(player.location))
            player.sendActionBar(Component.text("The teleport location for this plot has been updated", NamedTextColor.GREEN))
            true
        } else {
            player.playSound(player, "lom:fail_wrong", 1.0f, 1.0f)
            true
        }
    }

    override val tabCompleter: TabCompleter? = null
}
