package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import java.util.concurrent.TimeUnit

class AutopickupCommand : ICommandImpl {
    override val name: String = "autopickup"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        if (args.isEmpty()) {
            return@CommandExecutor false
        }
        if (args.size == 2 && !args[1].matches(Regex("\\d+"))) {
            return@CommandExecutor false
        }

        val playerName = args[0]
        val seconds = args.getOrNull(1)?.toIntOrNull() ?: 10
        val player = sender.server.getPlayer(playerName) ?: return@CommandExecutor false

        val task = Bukkit.getAsyncScheduler().runDelayed(Plugin.plugin, {
            Plugin.plugin.autoPickup[player]?.cancel()
            Plugin.plugin.autoPickup.remove(player)
        }, seconds.toLong(), TimeUnit.SECONDS)

        Plugin.plugin.autoPickup[player] = task
        true
    }

    override val tabCompleter: TabCompleter? = null
}
