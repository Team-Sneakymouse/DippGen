package com.danidipp.dippgen.Events

import com.danidipp.dippgen.Modules.PlotManagement.Plot
import com.danidipp.dippgen.Plugin
import com.danidipp.dippgen.Replacement
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import java.util.concurrent.ThreadLocalRandom
import java.util.logging.Level

class RequeueOnBlockBreak : Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        if (event.isCancelled) return

        val data = event.block.blockData
        val isTopHalf = data is Bisected && data.half == Bisected.Half.TOP
        val eventLocation: Location = if (isTopHalf) event.block.getRelative(BlockFace.DOWN).location else event.block.location
        val magicspellsMarker = eventLocation.block.getRelative(0, 200, 0).type == Material.BARRIER
        if (magicspellsMarker) return

        Plugin.plugin.replacements.forEach { replacement: Replacement ->
            val matchesLocation = replacement.locations.any { it == eventLocation }
            val matchesRegion = replacement.regions.any { region ->
                region.contains(eventLocation.blockX, eventLocation.blockY, eventLocation.blockZ)
            } && replacement.blocks.any { block ->
                block.material == eventLocation.block.type
            } && Plot.getPlot(eventLocation) == null

            if (!matchesLocation && !matchesRegion) return@forEach

            val minDelay = replacement.minDelay
            val maxDelay = replacement.maxDelay
            val delay = if (minDelay == maxDelay) minDelay else ThreadLocalRandom.current().nextLong(minDelay, maxDelay)

            if (!matchesLocation) {
                replacement.locations += eventLocation
                Plugin.plugin.config.set("replacements.${replacement.name}", replacement.toMap())
                Plugin.plugin.saveConfig()
            }

            val task = Runnable {
                Plugin.plugin.logger.log(Level.FINE, "Ran replacement task")
                replacement.placeBlock(eventLocation.block)
                if (!matchesLocation) {
                    replacement.locations.remove(eventLocation)
                    Plugin.plugin.config.set("replacements.${replacement.name}", replacement.toMap())
                    Plugin.plugin.saveConfig()
                }
            }
            Bukkit.getScheduler().runTaskLater(Plugin.plugin, task, delay)
        }
    }
}
