package com.danidipp.dippgen.Events

import com.danidipp.dippgen.Plugin
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAttemptPickupItemEvent

class ItemPickup : Listener {
    @EventHandler
    fun onItemPickup(event: PlayerAttemptPickupItemEvent) {
        if (Plugin.plugin.autoPickup.containsKey(event.player) || event.player.isSneaking) {
            return
        }

        event.isCancelled = true
        val item: Item = event.item
        item.pickupDelay = maxOf(5, item.pickupDelay)
    }
}
