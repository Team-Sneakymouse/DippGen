package com.danidipp.dippgen.Modules.PlotManagement

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object IconUtil {
    fun plotOwner(plot: Plot): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta

        val ownerUUID = plot.region.owners.uniqueIds.firstOrNull()
        val owner = ownerUUID?.let(Bukkit::getOfflinePlayer)
        val ownerName = owner?.name ?: "No Owner"
        val ownedPlots = Plot.getOwnedPlots(ownerUUID)

        if (owner != null) {
            meta.owningPlayer = owner
        }

        meta.displayName(Component.text("Plot Owner", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
        meta.lore(
            listOf(
                Component.text(ownerName, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false),
                Component.text("Plot slots used: ", NamedTextColor.WHITE)
                    .append(Component.text(ownedPlots.size, NamedTextColor.YELLOW))
                    .append(Component.text("/", NamedTextColor.GRAY))
                    .append(Component.text(Plot.getPlotLimit(owner), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false),
            ),
        )
        meta.persistentDataContainer.set(PlotDeed.plotIdKey, PersistentDataType.STRING, plot.id)
        item.itemMeta = meta
        return item
    }

    fun plotInfo(plot: Plot): ItemStack {
        val item = ItemStack(Material.BOOK)
        val meta = item.itemMeta

        val memberCount = plot.region.members.size()
        val ownerUuid: UUID? = plot.region.owners.uniqueIds.firstOrNull()
        val maxMembers = ownerUuid?.let { PlotManagementGUI.getMemberLimit(Bukkit.getOfflinePlayer(it)) } ?: 0

        meta.displayName(Component.text("Plot Info", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
        meta.lore(
            listOf(
                Component.text("District: ", NamedTextColor.WHITE)
                    .append(Component.text(plot.district?.name ?: "Unknown", NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC, false),
                Component.text("Members: ", NamedTextColor.WHITE)
                    .append(Component.text(memberCount, NamedTextColor.GOLD))
                    .append(Component.text("/", NamedTextColor.GRAY))
                    .append(Component.text(maxMembers, NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC, false),
            ),
        )

        meta.persistentDataContainer.set(PlotDeed.plotIdKey, PersistentDataType.STRING, plot.id)
        item.itemMeta = meta
        return item
    }

    fun abandonPlot(plot: Plot): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val meta = item.itemMeta
        meta.displayName(Component.text("Abandon Plot", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
        meta.lore(
            listOf(
                Component.text("Click to abandon this plot", NamedTextColor.YELLOW),
                Component.text("This will remove all members and", NamedTextColor.GRAY),
                Component.text("make the plot available for purchase.", NamedTextColor.GRAY),
            ).map { it.decoration(TextDecoration.ITALIC, false) },
        )
        meta.persistentDataContainer.set(PlotDeed.plotIdKey, PersistentDataType.STRING, plot.id)
        item.itemMeta = meta
        return item
    }

    fun memberPortrait(player: OfflinePlayer, plot: Plot, isOwner: Boolean): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta as SkullMeta
        if (player.name != null) {
            meta.owningPlayer = player
        }

        meta.displayName(Component.text(player.name ?: "unknown", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
        if (isOwner) {
            meta.lore(
                listOf(
                    Component.text("Click to remove them from the plot", NamedTextColor.YELLOW),
                    Component.text("Additional member slots can be", NamedTextColor.GRAY),
                    Component.text("acquired from Skill Trees", NamedTextColor.GRAY),
                ).map { it.decoration(TextDecoration.ITALIC, false) },
            )
        }

        meta.persistentDataContainer.set(PlotDeed.plotIdKey, PersistentDataType.STRING, plot.id)
        item.itemMeta = meta
        return item
    }

    fun addMember(plot: Plot): ItemStack {
        val item = ItemStack(Material.GREEN_STAINED_GLASS_PANE)
        val meta = item.itemMeta
        meta.displayName(Component.text("Add Member", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        meta.lore(
            listOf(
                Component.text("Additional member slots can be", NamedTextColor.GRAY),
                Component.text("acquired from Skill Trees", NamedTextColor.GRAY),
            ).map { it.decoration(TextDecoration.ITALIC, false) },
        )
        meta.persistentDataContainer.set(PlotDeed.plotIdKey, PersistentDataType.STRING, plot.id)
        item.itemMeta = meta
        return item
    }
}
