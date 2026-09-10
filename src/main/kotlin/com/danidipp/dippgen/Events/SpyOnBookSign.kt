package com.danidipp.dippgen.Events

import com.danidipp.dippgen.Plugin
import net.kyori.adventure.nbt.api.BinaryTagHolder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEditBookEvent

class SpyOnBookSign : Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerEditBookEvent(event: PlayerEditBookEvent) {
        if (!event.isSigning) return

        val bookMeta = event.newBookMeta
        val bookId = bookMeta.hashCode()
        Plugin.plugin.recentBooks[bookId.toString()] = bookMeta
        Bukkit.getScheduler().runTaskLater(
            Plugin.plugin,
            Runnable { Plugin.plugin.recentBooks.remove(bookId.toString()) },
            12000L,
        )

        val playerText = event.player.displayName()
            .color(NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(Component.text("Teleport to player")))
            .clickEvent(ClickEvent.runCommand("/minecraft:tp ${event.player.name}"))

        val bookNbt = BinaryTagHolder.binaryTagHolder(bookMeta.asString)
        val title = bookMeta.title() ?: Component.text("Untitled")
        val bookText = title
            .color(NamedTextColor.GOLD)
            .hoverEvent(HoverEvent.showText(Component.text("Signed book metadata: $bookNbt")))
            .clickEvent(ClickEvent.runCommand("/${Plugin.plugin.name}:showbook $bookId"))

        val message = Plugin.LOG_PREFIX
            .append(playerText)
            .append(Component.text(" signed book "))
            .append(bookText)

        Bukkit.getServer().onlinePlayers
            .filter { it.hasPermission("dipp.bookspy") }
            .forEach { it.sendMessage(message) }
    }
}
