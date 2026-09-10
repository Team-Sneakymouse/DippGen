package com.danidipp.dippgen.Events

import com.danidipp.dippgen.Plugin
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.FishHook
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent

class RDMMonitor : Listener {
    private fun limitPlayerDamage(player: Player): Boolean {
        if (player.hasPermission("dipp.rdmbypass")) return false
        if (player.hasPermission("dipp.rdmlimit")) return true
        if (player.javaClass.canonicalName?.contains(".entity.EntityHumanNPC.PlayerNPC") == true) return false

        val playtimeString = PlaceholderAPI.setPlaceholders(player, "%cmi_user_playtime_hourst%")
        val playtime = playtimeString.toFloatOrNull()
        if (playtime == null) {
            Plugin.plugin.logger.warning("Error parsing playtime: $playtimeString")
            return false
        }

        return playtime < 100f
    }

    @EventHandler
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return
        if (victim == damager) return
        if (limitPlayerDamage(damager)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBobberHit(event: ProjectileHitEvent) {
        if (event.entity !is FishHook) return
        if (event.hitEntity !is Player) return
        val hooker = event.entity.shooter as? Player ?: return
        if (limitPlayerDamage(hooker)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onKill(event: PlayerDeathEvent) {
        val killer = event.entity.killer ?: return
        val victim = event.entity

        val deathMessage = when (val message = event.deathMessage()) {
            null -> null
            is TextComponent -> message
            is TranslatableComponent -> Component.text(message.key())
            else -> Component.text(message.toString())
        }

        var killerText = killer.displayName().color(NamedTextColor.GOLD)
        if (deathMessage != null) {
            killerText = killerText.hoverEvent(
                HoverEvent.showText(deathMessage.append(Component.newline()).append(Component.text("Teleport to player"))),
            )
        }
        killerText = killerText.clickEvent(ClickEvent.runCommand("/minecraft:tp ${killer.name}"))

        var victimText = victim.displayName().color(NamedTextColor.GOLD)
        if (deathMessage != null) {
            victimText = victimText.hoverEvent(
                HoverEvent.showText(deathMessage.append(Component.newline()).append(Component.text("Teleport to player"))),
            )
        }
        victimText = victimText.clickEvent(ClickEvent.runCommand("/minecraft:tp ${victim.name}"))

        var locationText = Component.text("[TP]", NamedTextColor.YELLOW)
        if (deathMessage != null) {
            locationText = locationText.hoverEvent(
                HoverEvent.showText(deathMessage.append(Component.newline()).append(Component.text("Teleport to death position"))),
            )
        }
        locationText = locationText.clickEvent(
            ClickEvent.runCommand("/minecraft:tp ${victim.location.blockX} ${victim.location.blockY} ${victim.location.blockZ}"),
        )

        val text = Plugin.LOG_PREFIX
            .hoverEvent(if (deathMessage != null) HoverEvent.showText(deathMessage) else null)
            .append(killerText)
            .append(Component.text(" killed ", NamedTextColor.YELLOW))
            .append(victimText)
            .append(Component.space())
            .append(locationText)

        Bukkit.getServer().onlinePlayers
            .filter { it.isOp || it.hasPermission("dipp.rdmspy") }
            .forEach { it.sendMessage(text) }
    }
}
