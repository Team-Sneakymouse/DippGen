package com.danidipp.dippgen.Modules.PlotManagement

import com.danidipp.dippgen.Plugin
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType

object PlotManagementGUI {
    private val markerInventory = Bukkit.createInventory(null, 9)
    val plotManagementInventoryHolder: InventoryHolder = object : InventoryHolder {
        override fun getInventory(): Inventory = markerInventory
    }

    @JvmField
    val listener: Listener = object : Listener {
        private val addMemberTimers: MutableMap<Player, PlayerAddMemberInfo> = mutableMapOf()

        @EventHandler
        fun onInventoryClick(event: InventoryClickEvent) {
            if (event.inventory.holder != plotManagementInventoryHolder) return

            event.isCancelled = true
            if (event.clickedInventory?.holder != plotManagementInventoryHolder) return

            val item = event.currentItem ?: return
            val player = event.whoClicked as? Player ?: return
            val slot = event.slot
            val plotId = item.itemMeta.persistentDataContainer.get(PlotDeed.plotIdKey, PersistentDataType.STRING)
            val plot = plotId?.let(Plot::getPlot)
            if (plot == null) {
                player.sendMessage(Component.text("error: Plot \"$plotId\" not found. Please tell Dani!", NamedTextColor.RED))
                Bukkit.getScheduler().runTask(Plugin.plugin, Runnable { event.view.close() })
                return
            }

            val isOwner = plot.region.owners.uniqueIds.contains(player.uniqueId)
            val isManager = plot.isManager(player)

            when {
                slot == 8 && isOwner -> abandonPlot(event, plot)
                item.type == Material.PLAYER_HEAD && slot >= 9 && isManager -> removeMember(event, plot)
                item.type == Material.GREEN_STAINED_GLASS_PANE && isManager -> addMember(event, plot)
            }
        }

        private fun abandonPlot(event: InventoryClickEvent, plot: Plot) {
            val player = event.whoClicked as? Player ?: return
            plot.region.members.removeAll()
            plot.region.owners.removeAll()
            plot.region.setFlag(Plot.teleportLocationFlag, null)
            plot.region.setFlag(Plot.plotUnlockedFlag, false)
            player.inventory.setItemInMainHand(null)
            player.playSound(player, "lom:light.buff2", 0.5f, 1.0f)
            Bukkit.getScheduler().runTask(Plugin.plugin, Runnable { player.closeInventory() })
        }

        private fun addMember(event: InventoryClickEvent, plot: Plot) {
            val player = event.whoClicked as? Player ?: return
            val bossbarCommand = "cmi bossbarmsg ${player.name} &aTouch someone to add them to this plot -sec:-20 -n:addmember-${plot.id}"
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bossbarCommand)
            Bukkit.getScheduler().runTask(Plugin.plugin, Runnable { player.closeInventory() })
            val task = Bukkit.getScheduler().runTaskLater(Plugin.plugin, Runnable {
                addMemberTimers.remove(player)
            }, 400L)
            addMemberTimers[player] = PlayerAddMemberInfo(plot, task)
        }

        private fun removeMember(event: InventoryClickEvent, plot: Plot) {
            val player = event.whoClicked as? Player ?: return
            val item = event.currentItem ?: return
            val meta = item.itemMeta as? SkullMeta ?: return
            val target = meta.owningPlayer ?: return
            plot.region.members.removePlayer(target.uniqueId)
            Plugin.plugin.logger.info("removed member")
            Bukkit.getScheduler().runTask(Plugin.plugin, Runnable {
                player.openInventory(create(plot, player))
            })
        }

        @EventHandler
        fun onPlayerHitEvent(event: EntityDamageByEntityEvent) {
            if (event.damager.type != EntityType.PLAYER || event.entity.type != EntityType.PLAYER) return

            val player = event.damager as Player
            val target = event.entity as Player
            val info = addMemberTimers.remove(player) ?: return

            info.task.cancel()
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi bossbarmsg ${player.name} -cancel:addmember-${info.plot.id}")
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getPositionalPlaySoundCommand("lom:cute.hit", player, target.location))
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getPositionalPlaySoundCommand("lom:cute.hit", target, player.location))
            info.plot.region.members.addPlayer(target.uniqueId)
            player.openInventory(create(info.plot, player))
        }

        private fun getPositionalPlaySoundCommand(sound: String, listener: Player, location: Location): String {
            return "playsound $sound master ${listener.name} ${location.x} ${location.y} ${location.z}"
        }
    }

    fun getMemberLimit(player: OfflinePlayer): Int {
        if (!player.isConnected) {
            return 1
        }

        val maxMembersString = PlaceholderAPI.setPlaceholders(player, "%ms_variable_statPlotFriends%")
        return maxMembersString.toFloatOrNull()?.toInt() ?: 1
    }

    fun create(plot: Plot, player: Player): Inventory {
        val plotName = plot.region.id.split("-").getOrNull(1) ?: plot.region.id
        val maxMembers = getMemberLimit(player).coerceAtMost(45)
        val slots = 9 + (1 + maxMembers / 9) * 9

        val inventory = Bukkit.createInventory(
            plotManagementInventoryHolder,
            slots,
            Component.text("Plot Management: $plotName", NamedTextColor.GOLD),
        )

        inventory.setItem(0, IconUtil.plotOwner(plot))
        inventory.setItem(1, IconUtil.plotInfo(plot))

        val isOwner = plot.region.owners.uniqueIds.contains(player.uniqueId)
        if (isOwner) {
            inventory.setItem(8, IconUtil.abandonPlot(plot))
        }

        val memberUuids = plot.region.members.uniqueIds.toList()
        memberUuids.forEachIndexed { index, uuid ->
            inventory.setItem(9 + index, IconUtil.memberPortrait(Bukkit.getOfflinePlayer(uuid), plot, isOwner))
        }

        if (plot.isManager(player)) {
            for (i in memberUuids.size until getMemberLimit(player)) {
                inventory.setItem(9 + i, IconUtil.addMember(plot))
            }
        }

        return inventory
    }
}
