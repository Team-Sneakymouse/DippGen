package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Modules.PlotManagement.Plot
import com.danidipp.dippgen.Modules.PlotManagement.PlotDeed
import com.danidipp.dippgen.Plugin
import com.sk89q.worldedit.util.Location as WgLocation
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class HomestoneCommand : ICommandImpl, Listener {
    companion object {
        private val markerInventory = Bukkit.createInventory(null, 9)
        val inventoryHolder: InventoryHolder = object : InventoryHolder {
            override fun getInventory(): Inventory = markerInventory
        }

        fun createInventory(player: Player): Inventory {
            val plots = Plot.getOwnedPlots(player).toList()
            val inventorySlots = ((plots.size / 9) + 1) * 9
            val inventory = Bukkit.createInventory(
                inventoryHolder,
                inventorySlots,
                Component.text("Choose a plot to teleport to", NamedTextColor.GOLD),
            )

            plots.forEachIndexed { index, plot ->
                val item = ItemStack(plot.district?.material ?: Material.GRASS_BLOCK)
                val meta = item.itemMeta
                meta.displayName(Component.text(plot.name, NamedTextColor.YELLOW))
                meta.persistentDataContainer.set(PlotDeed.plotIdKey, PersistentDataType.STRING, plot.id)
                item.itemMeta = meta
                inventory.setItem(index, item)
            }

            return inventory
        }
    }

    override val name: String = "homestone"

    override val executor: CommandExecutor = CommandExecutor { _, _, _, args ->
        if (args.isEmpty()) {
            return@CommandExecutor false
        }

        val player = Bukkit.getPlayer(args[0]) ?: return@CommandExecutor false
        player.openInventory(createInventory(player))
        true
    }

    override val tabCompleter: TabCompleter? = null

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory.holder != inventoryHolder) {
            return
        }

        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (event.clickedInventory?.holder != inventoryHolder) {
            return
        }

        val item = event.currentItem ?: return
        val meta = item.itemMeta
        val plotId = meta.persistentDataContainer.get(PlotDeed.plotIdKey, PersistentDataType.STRING) ?: return
        val plot = Plot.getPlot(plotId)
        if (plot == null) {
            player.sendMessage(Component.text("Error: Couldn't get plot with id \"$plotId\". Please tell Dani!", NamedTextColor.RED))
            return
        }

        val tpLocation = plot.region.getFlag(Plot.teleportLocationFlag) as? WgLocation
        if (tpLocation == null) {
            player.sendMessage(Component.text("This plot doesn't have a teleport destination set up", NamedTextColor.RED))
            Bukkit.getScheduler().runTask(Plugin.plugin, Runnable { event.view.close() })
            return
        }

        val command = "ms cast as ${player.name} item-homestone-loop ${tpLocation.blockX} ${tpLocation.blockY} ${tpLocation.blockZ} ${tpLocation.yaw} ${tpLocation.pitch}"
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        Bukkit.getScheduler().runTask(Plugin.plugin, Runnable { event.view.close() })
    }
}
