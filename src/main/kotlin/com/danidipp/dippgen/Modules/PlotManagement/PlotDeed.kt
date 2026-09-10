package com.danidipp.dippgen.Modules.PlotManagement

import com.danidipp.dippgen.Plugin
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

data class PlotDeed(
    val name: String,
    val firstLore: String,
    val customModelData: Int,
) {
    companion object {
        val plotIdKey: NamespacedKey
            get() = NamespacedKey(Plugin.plugin, "plot-id")

        @JvmField
        val listener: Listener = object : Listener {
            @EventHandler
            fun onPlayerInteract(event: PlayerInteractEvent) {
                if ((event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) || event.hand == EquipmentSlot.OFF_HAND) {
                    return
                }

                val item = event.item ?: return
                if (item.type != Material.RABBIT_FOOT) {
                    return
                }

                val itemMeta = item.itemMeta
                val deedType = getType(itemMeta) ?: return
                event.isCancelled = true

                val player = event.player
                val currentPlot = Plot.getPlot(player.location)
                if (currentPlot == null) {
                    player.sendMessage(Component.text("error: You are not in a plot", NamedTextColor.RED))
                    return
                }

                when (deedType) {
                    DeedType.CLAIM -> {
                        val currentPlotDistrictId = currentPlot.region.id.split("-").firstOrNull()
                        val plotDistrict = District.districts.firstOrNull { it.id == currentPlotDistrictId }
                        val itemName = itemMeta.displayName()?.let(PlainTextComponentSerializer.plainText()::serialize)
                        val deedDistrict = District.districts.firstOrNull { it.deed.name == itemName }

                        if (plotDistrict == null || deedDistrict == null) {
                            player.sendMessage(Component.text("error: You can't use this deed here", NamedTextColor.RED))
                            return
                        }
                        if (currentPlot.region.owners.size() > 0) {
                            player.sendMessage(Component.text("error: This plot is already claimed", NamedTextColor.RED))
                            return
                        }
                        if (plotDistrict != deedDistrict) {
                            player.sendMessage(Component.text("error: This deed is not for this district", NamedTextColor.RED))
                            return
                        }

                        player.openInventory(PlotClaimGUI.create(currentPlot, player))
                    }
                    DeedType.MANAGEMENT -> {
                        val plotId = itemMeta.persistentDataContainer.get(plotIdKey, PersistentDataType.STRING)
                        if (plotId == null) {
                            player.sendMessage(
                                Component.text(
                                    "error: This management deed does not have a plot id. Please tell Dani!",
                                    NamedTextColor.RED,
                                ),
                            )
                            return
                        }
                        if (!currentPlot.region.id.endsWith(plotId)) {
                            player.sendMessage(Component.text("error: This deed is not for this plot", NamedTextColor.RED))
                            return
                        }

                        val wgPlayer = WorldGuardPlugin.inst().wrapOfflinePlayer(player)
                        val isOwner = currentPlot.region.owners.contains(wgPlayer)
                        val isMember = currentPlot.region.members.contains(wgPlayer)
                        if (!isOwner && !isMember) {
                            player.sendMessage(Component.text("error: You are not a member of this plot", NamedTextColor.RED))
                            return
                        }

                        player.openInventory(PlotManagementGUI.create(currentPlot, player))
                    }
                    DeedType.OPEN -> {
                        player.openInventory(PlotClaimGUI.create(currentPlot, player))
                    }
                }
            }
        }

        fun getType(meta: ItemMeta?): DeedType? {
            if (meta == null || !meta.hasDisplayName() || !meta.hasLore() || !meta.hasCustomModelData()) {
                return null
            }

            val itemName = meta.displayName()?.let(PlainTextComponentSerializer.plainText()::serialize) ?: return null
            val persistentData = meta.persistentDataContainer
            if (persistentData.has(plotIdKey, PersistentDataType.STRING)) {
                if (itemName.contains("[OPEN]")) return DeedType.OPEN
                if (itemName.contains(": ")) return DeedType.MANAGEMENT
            }

            val matchesDistrict = District.districts.asSequence().map { it.deed }.any { deed ->
                val nameMatch = itemName.startsWith(deed.name) || itemName.startsWith("Stick District")
                val modelDataMatch = meta.customModelData == deed.customModelData
                nameMatch && modelDataMatch
            }
            return if (matchesDistrict) DeedType.CLAIM else null
        }

        fun getDeedItem(plot: Plot, type: DeedType): ItemStack? {
            val item = ItemStack(Material.RABBIT_FOOT)
            val meta = item.itemMeta
            val districtId = plot.region.id.split("-").firstOrNull()
            val district = District.districts.firstOrNull { it.id == districtId } ?: return null
            val deed = district.deed
            meta.setCustomModelData(deed.customModelData)

            when (type) {
                DeedType.CLAIM -> {
                    meta.displayName(Component.text(deed.name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                    meta.lore(
                        listOf(
                            Component.text(deed.firstLore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        ),
                    )
                }
                DeedType.MANAGEMENT -> {
                    var ownerName = plot.region.owners.uniqueIds
                        .map { uuid -> Plugin.plugin.server.getOfflinePlayer(uuid) }
                        .mapNotNull(OfflinePlayer::getName)
                        .joinToString(", ")
                    if (ownerName.isEmpty()) {
                        ownerName = "n.a."
                    }

                    val plotLabel = plot.region.id.split("-").getOrNull(1) ?: plot.region.id
                    meta.displayName(
                        Component.text("${deed.name}: $plotLabel", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false),
                    )
                    meta.lore(
                        listOf(
                            Component.text("Leased to: ", NamedTextColor.YELLOW)
                                .append(Component.text(ownerName, NamedTextColor.GOLD))
                                .decoration(TextDecoration.ITALIC, false),
                            Component.text("Open to manage your plot", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false),
                        ),
                    )
                }
                DeedType.OPEN -> {
                    meta.displayName(Component.text(deed.name, NamedTextColor.GOLD))
                    meta.lore(listOf(Component.text("Dani forgot to update this!", NamedTextColor.GRAY)))
                }
            }

            meta.persistentDataContainer.set(plotIdKey, PersistentDataType.STRING, plot.region.id)
            item.itemMeta = meta
            return item
        }
    }

    enum class DeedType {
        CLAIM,
        MANAGEMENT,
        OPEN,
    }
}
