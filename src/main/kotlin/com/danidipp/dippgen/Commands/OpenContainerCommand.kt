package com.danidipp.dippgen.Commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.block.Container
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class OpenContainerCommand : ICommandImpl {
    override val name: String = "opencontainer"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        if (args.size != 4) {
            return@CommandExecutor false
        }

        val player = sender as? Player
        if (player == null) {
            sender.sendMessage(Component.text("error: Only players can use this command", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val world = Bukkit.getWorld(args[0])
        if (world == null) {
            player.sendMessage(Component.text("error: World ${args[0]} does not exist", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val x = args[1].toIntOrNull()
        val y = args[2].toIntOrNull()
        val z = args[3].toIntOrNull()
        if (x == null || y == null || z == null) {
            player.sendMessage(Component.text("error: Invalid coordinates", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val block = world.getBlockAt(x, y, z)
        val container = block.state as? Container
        if (container == null) {
            player.sendMessage(Component.text("error: Block is not a container", NamedTextColor.RED))
            return@CommandExecutor true
        }

        player.openInventory(container.inventory)
        true
    }

    override val tabCompleter: TabCompleter? = null
}
