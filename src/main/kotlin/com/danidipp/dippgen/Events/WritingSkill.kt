package com.danidipp.dippgen.Events

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.util.Location as WgLocation
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.flags.Flags
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.Tag
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.player.PlayerEditBookEvent
import org.bukkit.event.player.PlayerInteractEvent

class WritingSkill : Listener {
    @EventHandler
    fun onSignEdit(event: SignChangeEvent) {
        for (i in event.lines().indices) {
            val line = event.line(i) as? TextComponent ?: continue
            event.line(i, line.font(null))
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return
        if (!Tag.ALL_SIGNS.isTagged(clickedBlock.type)) return

        val signState = clickedBlock.state as? Sign ?: return
        val side = signState.getTargetSide(event.player)
        for (i in side.lines().indices) {
            val line = side.line(i) as? TextComponent ?: continue
            side.line(i, line.font(null))
        }

        val player = event.player
        val wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player)
        val canBypass = WorldGuard.getInstance().platform.sessionManager.hasBypass(wgPlayer, wgPlayer.world)
        if (canBypass) return

        val query = WorldGuard.getInstance().platform.regionContainer.createQuery()
        val wgLocation: WgLocation = BukkitAdapter.adapt(clickedBlock.location)
        if (!query.testState(wgLocation, wgPlayer, Flags.BUILD) ||
            !CheckDistrictOnBlockPlace.canDo(CheckDistrictOnBlockPlace.CheckAction.PLACE, player, clickedBlock.location, clickedBlock.type)
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBookEdit(event: PlayerEditBookEvent) {
        if (event.player.hasPermission("dipp.admin") || event.player.hasPermission("dipp.edit.book")) return
        event.isCancelled = true
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ms cast as ${event.player.name} general-books-error")
    }
}
