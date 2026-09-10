package com.danidipp.dippgen.Events

import com.danidipp.dippgen.Plugin
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class JoinSpy : Listener {
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        var message: String? = null

        if (!player.hasPlayedBefore()) {
            message = " joined the server for the first time! "
        } else {
            val playtime = PlaceholderAPI.setPlaceholders(player, "%cmi_user_playtime_hourst%")
            val hours = playtime.toDoubleOrNull()
            if (hours == null) {
                Plugin.plugin.logger.warning("Could not parse playtime for ${player.name}: $playtime")
            } else if (hours < 2.0) {
                message = " joined the server with $playtime hours of playtime. "
            }
        }

        if (message == null) {
            return
        }

        val playerComponent = player.displayName()
            .color(NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(Component.text("Teleport to player")))
            .clickEvent(ClickEvent.runCommand("/minecraft:tp ${player.name}"))

        val infoComponent = Component.text("[info]", NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(Component.text("Show player info")))
            .clickEvent(ClickEvent.runCommand("/cmi info ${player.name}"))

        val altComponent = Component.text("[alts]", NamedTextColor.YELLOW)
            .hoverEvent(HoverEvent.showText(Component.text("Show alts accounts")))
            .clickEvent(ClickEvent.runCommand("/cmi checkaccount ${player.name}"))

        val text = Plugin.LOG_PREFIX
            .append(Component.text("New player "))
            .append(playerComponent)
            .append(Component.text(message))
            .append(infoComponent)
            .append(Component.space())
            .append(altComponent)

        Plugin.plugin.server.onlinePlayers
            .filter { it.hasPermission("dipp.joinspy") }
            .forEach { it.sendMessage(text) }
    }
}
