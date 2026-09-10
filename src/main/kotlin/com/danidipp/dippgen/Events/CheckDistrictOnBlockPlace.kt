package com.danidipp.dippgen.Events

import com.danidipp.dippgen.Modules.PlotManagement.Plot
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Effect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTakeLecternBookEvent

class CheckDistrictOnBlockPlace : Listener {
    companion object {
        fun canDo(action: CheckAction, player: Player, location: Location, material: Material): Boolean =
            canDo(action.name.lowercase(), player, location, material.name.lowercase())

        fun canDo(action: CheckAction, player: Player, location: Location, entityType: EntityType): Boolean =
            canDo(action.name.lowercase(), player, location, entityType.name.lowercase())

        fun canDo(action: CheckAction, player: Player, location: Location, type: String): Boolean =
            canDo(action.name.lowercase(), player, location, type)

        private fun canDo(action: String, player: Player, location: Location, thing: String): Boolean {
            if (player.hasPermission("dipp.admin")) {
                return true
            }
            if (player.javaClass.canonicalName?.contains(".entity.EntityHumanNPC.PlayerNPC") == true) {
                return true
            }

            val regions = getRegions(location)
            if (regions.isEmpty()) {
                debugLog(player, NamedTextColor.RED, "[${action.uppercase()}] No regions found.")
                return false
            }

            val plot = Plot.getPlot(location)
            if (plot != null) {
                if (plot.isBuilder(player)) {
                    val permissionTemplate = "dipp.plot.{region}.$action.$thing"
                    val region = matchesRegion(player, permissionTemplate, regions)
                    if (region != null) {
                        debugLog(player, null, permissionTemplate.replace("{region}", region.id))
                        return true
                    }
                }
            } else {
                val permissionTemplate = "dipp.nonplot.{region}.$action.$thing"
                val region = matchesRegion(player, permissionTemplate, regions)
                if (region != null) {
                    debugLog(player, null, permissionTemplate.replace("{region}", region.id))
                    return true
                }
            }

            val permissionTemplate = "dipp.region.{region}.$action.$thing"
            val region = matchesRegion(player, permissionTemplate, regions)
            if (region != null) {
                debugLog(player, null, permissionTemplate.replace("{region}", region.id))
                return true
            }

            val regionsList = regions.joinToString(",") { it.id }
            debugLog(player, NamedTextColor.RED, "No permission: ${permissionTemplate.replace("{region}", "{$regionsList}")}")
            return false
        }

        private fun matchesRegion(
            player: Player,
            permissionTemplate: String,
            regions: List<ProtectedRegion>,
        ): ProtectedRegion? {
            regions.forEach { region ->
                val permission = permissionTemplate.replace("{region}", region.id)
                if (player.hasPermission(permission)) {
                    return region
                }
            }
            return null
        }

        private fun getRegions(location: Location): List<ProtectedRegion> {
            val wgLocation = BukkitAdapter.adapt(location)
            val query = WorldGuard.getInstance().platform.regionContainer.createQuery()
            return query.getApplicableRegions(wgLocation).regions.sortedByDescending { it.priority }
        }

        private fun playCancelEffect(player: Player?, location: Location?) {
            if (player == null || location == null) {
                return
            }
            player.playEffect(location, Effect.SMOKE, BlockFace.UP)
        }

        private fun debugLog(player: Player, color: TextColor?, message: String) {
            if (!player.hasPermission("dipp.debug")) {
                return
            }

            var text = Component.text(message)
            if (color != null) {
                text = text.color(color)
            }
            player.sendActionBar(text)
        }

        private fun isBypassDippGen(event: Event): Boolean {
            return runCatching {
                val method = event.javaClass.methods.firstOrNull {
                    it.name == "isBypassDippGen" && it.parameterCount == 0
                } ?: return@runCatching false
                method.invoke(event) as? Boolean ?: false
            }.getOrDefault(false)
        }

        private fun denyInteract(event: PlayerInteractEvent) {
            event.setUseInteractedBlock(Event.Result.DENY)
            event.setUseItemInHand(Event.Result.DENY)
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        if (isBypassDippGen(event)) {
            debugLog(event.player, NamedTextColor.WHITE, "Bypassed")
            return
        }

        if (!canDo(CheckAction.PLACE, event.player, event.block.location, event.blockPlaced.type)) {
            playCancelEffect(event.player, event.block.location)
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        if (isBypassDippGen(event)) {
            debugLog(event.player, NamedTextColor.WHITE, "Bypassed")
            return
        }

        if (!canDo(CheckAction.BREAK, event.player, event.block.location, event.block.type)) {
            playCancelEffect(event.player, event.block.location)
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onInteractBlock(event: PlayerInteractEvent) {
        if (isBypassDippGen(event)) {
            debugLog(event.player, NamedTextColor.WHITE, "Bypassed")
            return
        }
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        val material = clickedBlock.type
        val isInteractable = material.isInteractable && !Tag.STAIRS.isTagged(material) && !Tag.FENCES.isTagged(material)
        if (!isInteractable) {
            if (event.player.hasPermission("dipp.debug.extra")) {
                event.player.sendMessage(Component.text("Not interactable: $material", NamedTextColor.GRAY))
            }
            return
        }

        if (event.isBlockInHand && event.player.isSneaking) {
            return
        }

        if (!canDo(CheckAction.INTERACT, event.player, clickedBlock.location, clickedBlock.type)) {
            playCancelEffect(event.player, clickedBlock.location)
            denyInteract(event)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (isBypassDippGen(event)) {
            debugLog(event.player, NamedTextColor.WHITE, "Bypassed")
            return
        }

        if (!canDo(CheckAction.INTERACT_ENTITY, event.player, event.rightClicked.location, event.rightClicked.type)) {
            if (event.rightClicked is LivingEntity && event.rightClicked.type != EntityType.ARMOR_STAND) {
                if (!canDo(CheckAction.DAMAGE_ENTITY, event.player, event.rightClicked.location, "livingentity")) {
                    playCancelEffect(event.player, event.rightClicked.location)
                    event.isCancelled = true
                }
            } else {
                playCancelEffect(event.player, event.rightClicked.location)
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteractAtEntity(event: PlayerInteractAtEntityEvent) {
        if (isBypassDippGen(event)) {
            debugLog(event.player, NamedTextColor.WHITE, "Bypassed")
            return
        }

        onInteractEntity(event)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onInteractItem(event: PlayerInteractEvent) {
        if (isBypassDippGen(event)) {
            debugLog(event.player, NamedTextColor.WHITE, "Bypassed")
            return
        }
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        val item = event.item ?: return
        if (!item.type.isItem) {
            return
        }

        if (!canDo(CheckAction.INTERACT_ITEM, event.player, clickedBlock.location, item.type)) {
            playCancelEffect(event.player, clickedBlock.location)
            denyInteract(event)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onDamageEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        if (isBypassDippGen(event)) {
            debugLog(damager, NamedTextColor.WHITE, "Bypassed")
            return
        }

        if (!canDo(CheckAction.DAMAGE_ENTITY, damager, event.entity.location, event.entity.type)) {
            if (event.entity is LivingEntity && event.entity.type != EntityType.ARMOR_STAND) {
                if (!canDo(CheckAction.DAMAGE_ENTITY, damager, event.entity.location, "livingentity")) {
                    playCancelEffect(damager, event.entity.location)
                    event.isCancelled = true
                }
            } else {
                playCancelEffect(damager, event.entity.location)
                event.isCancelled = true
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onItemFrameBreak(event: HangingBreakByEntityEvent) {
        var player: Player? = null
        when (val remover = event.remover) {
            is Player -> player = remover
            is Projectile -> {
                val shooter = remover.shooter
                if (shooter !is Player) {
                    playCancelEffect(player, event.entity.location)
                    event.isCancelled = true
                    return
                }
                player = shooter
            }
        }

        if (isBypassDippGen(event)) {
            if (player != null) {
                debugLog(player, NamedTextColor.WHITE, "Bypassed")
            }
            return
        }

        if (player == null || !canDo(CheckAction.BREAK, player, event.entity.location, "item_frame")) {
            playCancelEffect(player, event.entity.location)
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onItemFramePlace(event: HangingPlaceEvent) {
        val player = event.player ?: return
        if (isBypassDippGen(event)) {
            debugLog(player, NamedTextColor.WHITE, "Bypassed")
            return
        }

        if (!canDo(CheckAction.PLACE, player, event.entity.location, "item_frame")) {
            playCancelEffect(player, event.block.location)
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onLecternPlaceEvent(event: PlayerInteractEvent) {
        if (isBypassDippGen(event)) {
            debugLog(event.player, NamedTextColor.WHITE, "Bypassed")
            return
        }
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        if (clickedBlock.type != Material.LECTERN) {
            return
        }

        if (!canDo(CheckAction.INTERACT, event.player, clickedBlock.location, clickedBlock.type)) {
            playCancelEffect(event.player, clickedBlock.location)
            denyInteract(event)
        }
    }

    @EventHandler
    fun onLecternTakeEvent(event: PlayerTakeLecternBookEvent) {
        if (!canDo(CheckAction.INTERACT, event.player, event.lectern.location, event.lectern.type)) {
            playCancelEffect(event.player, event.lectern.location.add(0.0, 1.0, 0.0))
            event.isCancelled = true
        }
    }

    enum class CheckAction {
        PLACE,
        BREAK,
        INTERACT,
        INTERACT_ENTITY,
        INTERACT_ITEM,
        DAMAGE_ENTITY,
    }
}
