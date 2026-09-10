package com.danidipp.dippgen.Modules.PlotManagement

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object ItemUtil {
    fun generateIcon(
        material: Material,
        name: Component,
        lore: List<Component>,
        customModelData: Int,
        plot: Plot,
    ): ItemStack {
        val icon = ItemStack(material)
        val meta = icon.itemMeta
        meta.displayName(name)
        meta.lore(lore)
        meta.setCustomModelData(customModelData)
        meta.persistentDataContainer.set(PlotDeed.plotIdKey, PersistentDataType.STRING, plot.id)
        icon.itemMeta = meta
        return icon
    }
}
