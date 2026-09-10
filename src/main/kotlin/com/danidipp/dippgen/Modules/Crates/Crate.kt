package com.danidipp.dippgen.Modules.Crates

import com.danidipp.dippgen.Plugin
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.entity.Shulker
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID
import java.util.stream.Collectors
import java.util.stream.Stream

class Crate : InventoryHolder {
    companion object {
        private val crateMap: MutableMap<UUID, Crate> = mutableMapOf()

        @JvmField
        val listener: Listener = object : Listener {
            @EventHandler
            fun onShulkerDeath(event: EntityDeathEvent) {
                val shulker = event.entity as? Shulker ?: return
                if (!shulker.scoreboardTags.contains("dipp.crate")) {
                    return
                }

                val crate = get(shulker)
                if (crate == null) {
                    Plugin.plugin.logger.warning("Crate not found for shulker ${shulker.uniqueId}")
                    return
                }

                event.drops.clear()
                event.droppedExp = 0
                crate.poof()
            }

            @EventHandler
            fun onShulkerBoxPlace(event: BlockPlaceEvent) {
                if (!Tag.SHULKER_BOXES.isTagged(event.block.type)) {
                    return
                }

                if (!event.player.hasPermission("dipp.admin") || !event.player.isSneaking || event.hand != EquipmentSlot.OFF_HAND) {
                    event.isCancelled = true
                    val item = event.itemInHand.clone()
                    val location = event.block.location
                    val direction = event.player.facing
                    Crate(item, location, direction)

                    if (event.player.gameMode != GameMode.CREATIVE) {
                        val inventory = event.player.inventory
                        if (inventory.itemInMainHand == item) {
                            inventory.setItemInMainHand(ItemStack(Material.AIR))
                        } else if (inventory.itemInOffHand == item) {
                            inventory.setItemInOffHand(ItemStack(Material.AIR))
                        }
                    }
                }
            }

            @EventHandler
            fun onCrateInteract(event: PlayerInteractEntityEvent) {
                if (event.hand != EquipmentSlot.HAND) {
                    return
                }

                val shulker = event.rightClicked as? Shulker ?: return
                if (!shulker.scoreboardTags.contains("dipp.crate")) {
                    return
                }

                event.isCancelled = true
                val crate = get(shulker) ?: Crate(shulker)
                event.player.openInventory(crate.inventory)
            }

            @EventHandler
            fun onHit(event: PrePlayerAttackEntityEvent) {
                val shulker = event.attacked as? Shulker ?: return
                if (!shulker.scoreboardTags.contains("dipp.crate")) {
                    return
                }

                val crate = get(shulker) ?: Crate(shulker)
                Bukkit.getScheduler().runTask(Plugin.plugin, Runnable {
                    if (crate.isValid()) {
                        crate.poof()
                    }
                })
            }
        }

        fun get(shulker: Shulker): Crate? = crateMap[shulker.uniqueId]

        fun get(uuid: UUID): Crate? = crateMap[uuid]

        fun get(backpack: Any): Crate? {
            val sourceId = backpackSourceId(backpack) ?: return null
            return crateMap[UUID(0L, sourceId.toLong())]
        }

        private fun backpackSourceId(backpack: Any): Int? {
            val sourceId = runCatching { backpack.javaClass.getMethod("getSourceId").invoke(backpack) }.getOrNull()
            return (sourceId as? Number)?.toInt()
        }

        private fun backpackPlayer(backpack: Any): Player? {
            return runCatching { backpack.javaClass.getMethod("getPlayer").invoke(backpack) as? Player }.getOrNull()
        }

        private fun backpackShulkerBox(backpack: Any): ItemStack? {
            return runCatching { backpack.javaClass.getMethod("getShulkerBox").invoke(backpack) as? ItemStack }.getOrNull()
        }
    }

    private var type: CrateType
    private var inventoryInternal: Inventory
    private var backpack: Any?
    private var shulker: Shulker?
    private var itemDisplay: ItemDisplay?

    constructor(backpack: Any) {
        type = CrateType.BACKPACK
        inventoryInternal = inventoryFromShulkerbox(backpackShulkerBox(backpack))
            ?: Bukkit.createInventory(this, 36, Component.text("Crate"))
        this.backpack = backpack
        shulker = null
        itemDisplay = null
        val sourceId = backpackSourceId(backpack)
        if (sourceId != null) {
            crateMap[UUID(0L, sourceId.toLong())] = this
        }
    }

    constructor(item: ItemStack, location: Location, direction: BlockFace) {
        type = CrateType.SHULKER
        backpack = null
        itemDisplay = null
        item.amount = 1
        val itemMeta = item.itemMeta
        val name = if (itemMeta?.hasDisplayName() == true) itemMeta.displayName() else null
        inventoryInternal = inventoryFromShulkerbox(item) ?: Bukkit.createInventory(this, 36, Component.text("Crate"))

        val createdShulker = CrateUtils.summonShulker(location, name, CrateUtils.getColor(item.type))
        shulker = createdShulker
        createdShulker.equipment?.setHelmet(item, true)
        createdShulker.equipment?.helmetDropChance = 1.0f
        itemDisplay = CrateUtils.summonItemDisplay(location, direction, item, createdShulker)
        crateMap[createdShulker.uniqueId] = this
    }

    constructor(shulker: Shulker) {
        type = CrateType.SHULKER
        this.shulker = shulker
        backpack = null
        itemDisplay = shulker.getNearbyEntities(0.5, 0.5, 0.5)
            .asSequence()
            .filterIsInstance<ItemDisplay>()
            .firstOrNull { it.scoreboardTags.contains("dipp.crate") }

        val item = shulker.equipment?.helmet
        if (item == null) {
            Plugin.plugin.logger.severe("Shulker at ${shulker.location} has no helmet!")
            inventoryInternal = Bukkit.createInventory(this, 36, Component.text("Crate"))
        } else {
            inventoryInternal = inventoryFromShulkerbox(item) ?: run {
                Plugin.plugin.logger.severe("Couldn't get inventory from shulker at ${shulker.location}")
                Bukkit.createInventory(this, 36, Component.text("Crate"))
            }
            crateMap[shulker.uniqueId] = this
        }
    }

    fun isValid(): Boolean {
        return if (type == CrateType.SHULKER) {
            shulker?.isValid == true
        } else {
            val backpack = backpack ?: return false
            val player = backpackPlayer(backpack) ?: return false
            val itemInHand = player.inventory.itemInMainHand
            val expected = backpackShulkerBox(backpack)
            if (!Tag.SHULKER_BOXES.isTagged(itemInHand.type)) {
                Plugin.plugin.logger.warning("Player ${player.name} has no shulkerbox in main hand!")
                false
            } else if (expected == null || itemInHand != expected) {
                Plugin.plugin.logger.warning("Player ${player.name} has different shulkerbox in main hand!")
                false
            } else {
                true
            }
        }
    }

    private fun inventoryFromShulkerbox(item: ItemStack?): Inventory? {
        if (item == null || !Tag.SHULKER_BOXES.isTagged(item.type)) {
            Plugin.plugin.logger.warning("Requested inventory from non-shulkerbox item!")
            return null
        }
        val itemMeta = item.itemMeta
        if (itemMeta !is BlockStateMeta || itemMeta.blockState !is ShulkerBox) {
            Plugin.plugin.logger.warning("Requested inventory from non-shulkerbox item!")
            return null
        }

        val shulkerBox = itemMeta.blockState as ShulkerBox
        val name = itemMeta.displayName() ?: Component.translatable(item.type.translationKey())
        val inventory = Bukkit.createInventory(this, 36, name)

        val oldContents = shulkerBox.inventory.contents
        val contents = arrayOfNulls<ItemStack>(36)
        for (i in 0 until 36) {
            if (i < 27) {
                contents[i] = oldContents[i]
            }
            if (contents[i] == null) {
                contents[i] = CrateGUI.getFiller(i, shulkerBox.type)
            }
        }

        inventory.contents = contents
        return inventory
    }

    fun save() {
        val contents = Stream.of(*inventoryInternal.contents)
            .limit(27)
            .map { stack ->
                if (stack == null || (!stack.isSimilar(CrateGUI.fillerItem) && stack.type != Material.JIGSAW)) stack else null
            }
            .collect(Collectors.toList())
            .toTypedArray()

        val item = if (type == CrateType.BACKPACK) {
            backpack?.let { backpackShulkerBox(it) }
        } else {
            shulker?.equipment?.helmet
        }

        if (item != null && Tag.SHULKER_BOXES.isTagged(item.type)) {
            val itemMeta = item.itemMeta as? BlockStateMeta
            val shulkerState = itemMeta?.blockState as? ShulkerBox
            if (itemMeta != null && shulkerState != null) {
                shulkerState.inventory.contents = contents
                shulkerState.update()
                itemMeta.blockState = shulkerState
                item.itemMeta = itemMeta

                if (type == CrateType.BACKPACK) {
                    backpack?.let { backpackPlayer(it) }?.inventory?.setItemInMainHand(item)
                } else {
                    shulker?.equipment?.setHelmet(item, true)
                }
            } else {
                val locationString = if (type == CrateType.BACKPACK) {
                    backpack?.let { backpackPlayer(it)?.name }?.let { "$it's backpack" } ?: "unknown backpack"
                } else {
                    shulker?.location.toString()
                }
                Plugin.plugin.logger.severe("Non-shulkerbox item in Shulker at $locationString!")
                CrateUtils.trySaveContents(contents, uuid.toString())
            }
        } else {
            Plugin.plugin.logger.severe("Non-shulkerbox item in Shulker at $location!")
        }
    }

    fun poof() {
        if (type == CrateType.BACKPACK) {
            val player = backpack?.let { backpackPlayer(it) }
            Plugin.plugin.logger.severe("Poof: ${player?.name} tried to poof a backpack!")
            player?.sendMessage(Component.text("You can't poof a backpack!", NamedTextColor.RED))
            return
        }

        inventoryInternal.viewers.toList().forEach { it.closeInventory() }
        val item = shulkerbox
        val shulker = shulker
        if (item != null && shulker != null) {
            shulker.world.dropItem(shulker.location, item)
            crateMap.remove(shulker.uniqueId)
            itemDisplay?.remove()
            shulker.world.playSound(shulker.location, Sound.ENTITY_CHICKEN_EGG, SoundCategory.BLOCKS, 1.0f, 0.5f)
            shulker.remove()
        } else {
            Plugin.plugin.logger.severe("Poof: Can't get shulkerbox item!")
        }
    }

    override fun getInventory(): Inventory = inventoryInternal

    fun getShulker(): Shulker? = shulker

    val shulkerbox: ItemStack?
        get() {
            save()
            return if (type == CrateType.BACKPACK) {
                backpack?.let { backpackShulkerBox(it) }
            } else {
                shulker?.equipment?.helmet
            }
        }

    val label: ItemStack?
        get() {
            val item = shulkerbox ?: return null
            val itemMeta: ItemMeta = item.itemMeta
            return if (itemMeta.hasCustomModelData()) {
                CrateUtils.getLabel(item.type, itemMeta.customModelData)
            } else {
                null
            }
        }

    val location: Location
        get() = if (type == CrateType.BACKPACK) {
            backpack?.let { backpackPlayer(it)?.location } ?: Bukkit.getWorlds()[0].spawnLocation
        } else {
            shulker?.location ?: Bukkit.getWorlds()[0].spawnLocation
        }

    val uuid: UUID
        get() = if (type == CrateType.BACKPACK) {
            val sourceId = backpack?.let { backpackSourceId(it) } ?: 0
            UUID(0L, sourceId.toLong())
        } else {
            shulker?.uniqueId ?: UUID.randomUUID()
        }
}
