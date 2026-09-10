package com.danidipp.dippgen.Modules.PlotManagement

import com.danidipp.dippgen.Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.ceil
import kotlin.math.max

object PlotClaimGUI {
    private val markerInventory = Bukkit.createInventory(null, 9)
    val plotClaimInventoryHolder: InventoryHolder = object : InventoryHolder {
        override fun getInventory(): Inventory = markerInventory
    }

    @JvmField
    val listener: Listener = object : Listener {
        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            if (event.inventory.holder != plotClaimInventoryHolder) return

            event.isCancelled = true
            if (event.clickedInventory?.holder != plotClaimInventoryHolder) return

            val item = event.currentItem ?: return
            when (item.type) {
                Material.WHITE_STAINED_GLASS_PANE -> clickNewPlot(event)
                Material.RED_STAINED_GLASS_PANE -> clickLockedSlot(event)
                Material.RABBIT_FOOT -> clickExistingPlot(event)
                else -> Plugin.plugin.logger.warning("Unknown item type in PlotClaimInventory: ${item.type}")
            }
        }

        private fun clickExistingPlot(event: InventoryClickEvent) {
            val player = event.whoClicked as? Player ?: return
            player.playSound(player, "lom:fail_wrong", 0.5f, 1.0f)
        }

        private fun clickNewPlot(event: InventoryClickEvent) {
            val player = event.whoClicked as? Player ?: return
            val item = event.currentItem ?: return
            val plotId = item.itemMeta.persistentDataContainer.get(PlotDeed.plotIdKey, PersistentDataType.STRING)
            val plot = plotId?.let(Plot::getPlot)
            if (plot == null) {
                player.sendMessage(Component.text("error: Plot \"$plotId\" not found. Please tell Dani!", NamedTextColor.RED))
                Bukkit.getScheduler().runTask(Plugin.plugin, Runnable { event.view.close() })
                return
            }

            if (plot.region.owners.size() > 0) {
                player.sendMessage(
                    Component.text(
                        "error: This plot is already claimed. If you believe this is an error, please tell Dani!",
                        NamedTextColor.RED,
                    ),
                )
                return
            }

            plot.region.owners.addPlayer(player.uniqueId)
            player.playSound(player, "lom:light.buff2", 0.5f, 1.0f)
            val deedItem = PlotDeed.getDeedItem(plot, PlotDeed.DeedType.MANAGEMENT)
            player.inventory.setItem(player.inventory.heldItemSlot, deedItem)
            Bukkit.getScheduler().runTask(Plugin.plugin, Runnable {
                player.openInventory(PlotManagementGUI.create(plot, player))
            })
        }

        private fun clickLockedSlot(event: InventoryClickEvent) {
            val player = event.whoClicked as? Player ?: return
            player.playSound(player, "lom:fail_wrong", 0.5f, 1.0f)
        }
    }

    fun create(plot: Plot, player: Player): Inventory {
        val plots = Plot.getOwnedPlots(player)
        val personalPlotLimit = if (player.hasPermission("dipp.admin")) plots.size + 1 else Plot.getPlotLimit(player)
        val maxUnlockablePlots = 4
        val inventoryRows = ceil(max(personalPlotLimit, maxUnlockablePlots) / 9.0).toInt().coerceAtLeast(1)

        val inventory = Bukkit.createInventory(
            plotClaimInventoryHolder,
            inventoryRows * 9,
            Component.text("Choose a plot slot (${plots.size}/$personalPlotLimit used)", NamedTextColor.GOLD),
        )

        var slot = 0
        plots.forEach { ownedPlot ->
            val district = ownedPlot.district ?: return@forEach
            val iconName = Component.text("${district.deed.name}: ${ownedPlot.name}")
            val iconLore = listOf(Component.text(district.name, NamedTextColor.GRAY))
            val iconItem = ItemUtil.generateIcon(Material.RABBIT_FOOT, iconName, iconLore, district.deed.customModelData, ownedPlot)
            inventory.setItem(slot, iconItem)
            slot++
        }

        for (i in slot until personalPlotLimit.coerceAtLeast(slot)) {
            val iconName = Component.text("Empty Slot", NamedTextColor.DARK_GREEN)
            val iconLore = listOf(Component.text("Click here to claim this plot", NamedTextColor.GRAY))
            val iconItem = ItemUtil.generateIcon(Material.WHITE_STAINED_GLASS_PANE, iconName, iconLore, 0, plot)
            inventory.setItem(i, iconItem)
        }

        for (i in personalPlotLimit.coerceAtLeast(0) until 4) {
            val iconName = Component.text("Locked Slot", NamedTextColor.DARK_RED)
            val iconLore = listOf(Component.text("Gain additional plot slots from your skill trees", NamedTextColor.GRAY))
            val iconItem = ItemUtil.generateIcon(Material.RED_STAINED_GLASS_PANE, iconName, iconLore, 0, plot)
            inventory.setItem(i, iconItem)
        }

        return inventory
    }
}
