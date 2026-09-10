package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ShowBookCommand : ICommandImpl {
    override val name: String = "showbook"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        if (args.isEmpty()) {
            return@CommandExecutor false
        }

        val player = sender as? Player
        if (player == null) {
            sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val bookMeta = Plugin.plugin.recentBooks[args[0]]
        if (bookMeta == null) {
            sender.sendMessage(Component.text("Error - Book lookup expired", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val book = ItemStack(Material.WRITTEN_BOOK)
        book.itemMeta = bookMeta
        player.openBook(book)
        true
    }

    override val tabCompleter: TabCompleter? = null
}
