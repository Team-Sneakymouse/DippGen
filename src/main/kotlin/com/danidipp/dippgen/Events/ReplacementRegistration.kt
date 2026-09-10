package com.danidipp.dippgen.Events

import com.danidipp.dippgen.Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

class ReplacementRegistration : Listener {
    @EventHandler
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        val item: ItemStack = event.player.inventory.itemInMainHand
        val displayName = item.itemMeta?.displayName()?.let(PlainTextComponentSerializer.plainText()::serialize) ?: return
        val nameParts = displayName.split(":")
        if (nameParts.size != 2 || nameParts[0] != "dipp") return

        val replacementName = nameParts[1]
        val replacement = Plugin.plugin.replacements.find { it.name == replacementName }
        if (replacement == null) {
            event.player.sendMessage(Component.text("Can't find replacement for \"$replacementName\"", NamedTextColor.RED))
            return
        }
        if (!event.player.hasPermission("dipp.register.$replacementName")) {
            event.player.sendMessage(
                Component.text(
                    "You don't have permission to register this replacement: dipp.register.$replacementName",
                    NamedTextColor.RED,
                ),
            )
            return
        }

        val target = event.block.location
        val coordinates = "${target.blockX} ${target.blockY} ${target.blockZ}"
        val existed = replacement.locations.any { locationMatches(it, target) }
        if (existed) {
            replacement.locations.removeIf { locationMatches(it, target) }
            Plugin.plugin.config.set("replacements.${replacement.name}", replacement.toMap())
            Plugin.plugin.saveConfig()
            target.block.type = Material.AIR
            event.player.sendMessage(Component.text("Removed registration from $coordinates", NamedTextColor.GREEN))
            event.isCancelled = true
        } else {
            event.player.sendMessage(Component.text("No registrations at location $coordinates", NamedTextColor.YELLOW))
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        val item: ItemStack = event.itemInHand
        val displayName = item.itemMeta?.displayName()?.let(PlainTextComponentSerializer.plainText()::serialize) ?: return
        val nameParts = displayName.split(":")
        if (nameParts.size != 2 || nameParts[0] != "dipp") return

        val replacementName = nameParts[1]
        val replacement = Plugin.plugin.replacements.find { it.name == replacementName }
        if (replacement == null) {
            event.player.sendMessage(Component.text("Can't find replacement for \"$replacementName\"", NamedTextColor.RED))
            return
        }
        if (!event.player.hasPermission("dipp.register.$replacementName")) {
            event.player.sendMessage(
                Component.text(
                    "You don't have permission to register this replacement: dipp.register.$replacementName",
                    NamedTextColor.RED,
                ),
            )
            return
        }

        val target = event.block.location
        val coordinates = "${target.blockX} ${target.blockY} ${target.blockZ}"
        if (replacement.locations.any { locationMatches(it, target) }) {
            event.player.sendMessage(Component.text("There is already a registration at this location", NamedTextColor.RED))
            event.isCancelled = true
            return
        }

        replacement.locations += target
        Plugin.plugin.config.set("replacements.${replacement.name}", replacement.toMap())
        Plugin.plugin.saveConfig()
        replacement.placeBlock(target.block)
        event.player.sendMessage(Component.text("Added $coordinates as replacement", NamedTextColor.GREEN))
    }

    private fun locationMatches(a: Location, b: Location): Boolean {
        return a.world == b.world && a.blockX == b.blockX && a.blockY == b.blockY && a.blockZ == b.blockZ
    }
}
