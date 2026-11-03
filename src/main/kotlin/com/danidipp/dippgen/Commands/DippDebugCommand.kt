package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.DebugMode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/** Toggles permission debug messages for the sender until they log out. */
class DippDebugCommand : ICommandImpl, Listener {
    override val name: String = "dippdebug"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, _ ->
        if (sender !is Player) {
            sender.sendMessage(Component.text("Only players can toggle debug messages.", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val enabled = DebugMode.toggle(sender.uniqueId)
        sender.sendMessage(
            if (enabled) {
                Component.text("Permission debug messages enabled until you log out.", NamedTextColor.GREEN)
            } else {
                Component.text("Permission debug messages disabled.", NamedTextColor.GRAY)
            },
        )
        true
    }

    override val tabCompleter: TabCompleter = TabCompleter { _, _, _, _ -> emptyList() }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        DebugMode.disable(event.player.uniqueId)
    }
}
