package com.danidipp.dippgen.Commands

import com.danidipp.dippgen.Modules.Crates.Crate
import com.danidipp.dippgen.Modules.Crates.CrateUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabCompleter
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class CratecheckCommand : ICommandImpl {
    override val name: String = "cratecheck"

    override val executor: CommandExecutor = CommandExecutor { sender, _, _, args ->
        if (args.size != 1) {
            return@CommandExecutor false
        }

        val player = sender.server.getPlayer(args[0])
        if (player == null) {
            sender.sendMessage(Component.text("Player not found", NamedTextColor.RED))
            return@CommandExecutor true
        }

        val shulkerBoxItem = player.inventory.itemInMainHand
        if (!Tag.SHULKER_BOXES.isTagged(shulkerBoxItem.type)) {
            sender.sendMessage(player.name().append(Component.text(" is not holding a shulker box in their main hand.", NamedTextColor.RED)))
            return@CommandExecutor true
        }

        val shulkerMaterial = shulkerBoxItem.type
        val shulkerModelData = shulkerBoxItem.itemMeta?.customModelData ?: 0
        val shulkerLabel = CrateUtils.getLabel(shulkerMaterial, shulkerModelData)
        if (shulkerLabel == null) {
            sender.sendMessage(player.name().append(Component.text(" is not holding a valid crate.", NamedTextColor.RED)))
            return@CommandExecutor true
        }

        val boxMeta = (shulkerBoxItem.itemMeta as? BlockStateMeta)?.blockState as? org.bukkit.block.ShulkerBox
        if (boxMeta == null) {
            sender.sendMessage(Component.text("Could not retrieve shulker box state.", NamedTextColor.RED))
            return@CommandExecutor true
        }

        if (validateStacks(boxMeta.inventory.contents, shulkerLabel)) {
            val itemMeta = shulkerBoxItem.itemMeta
            val persistentKey = itemMeta.persistentDataContainer.keys.firstOrNull { key ->
                key.toString() == "cmilib:cmibackpack"
            }
            val id = persistentKey?.let { itemMeta.persistentDataContainer.get(it, PersistentDataType.INTEGER) }
            val crate = id?.let { Crate.get(UUID(0L, it.toLong())) }
            if (crate != null && crate.inventory.viewers.isNotEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ms cast as ${player.name} item-crate-cratepack-fail")
                return@CommandExecutor true
            }

            player.inventory.setItemInMainHand(ItemStack(Material.AIR))
            val crateName = shulkerLabel.itemMeta.displayName()?.let(PlainTextComponentSerializer.plainText()::serialize) ?: "cratepack"
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ms cast as ${player.name} item-crate-$crateName-success")
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ms cast as ${player.name} item-crate-cratepack-fail")
        }

        true
    }

    private fun validateStacks(contents: Array<ItemStack?>, label: ItemStack): Boolean {
        var validStacks = 0

        contents.forEach { stack ->
            if (stack == null || stack.type == Material.AIR) {
                return@forEach
            }

            val materialCorrect = stack.type == label.type
            val modelDataCorrect = if (
                stack.hasItemMeta() &&
                label.hasItemMeta() &&
                stack.itemMeta.hasCustomModelData() &&
                label.itemMeta.hasCustomModelData()
            ) {
                stack.itemMeta.customModelData == label.itemMeta.customModelData
            } else {
                true
            }

            if (!materialCorrect || !modelDataCorrect || stack.amount != 99) {
                return false
            }

            validStacks++
        }

        return validStacks == 9
    }

    override val tabCompleter: TabCompleter? = null
}
