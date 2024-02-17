package com.danidipp.dippgen.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

import com.danidipp.dippgen.Plugin;

public class ItemPickup implements Listener {
	@EventHandler
	public void onItemPickup(PlayerAttemptPickupItemEvent event) {
		Plugin.plugin.getLogger().info("Item pickup event");
		if (!event.getPlayer().isSneaking()) {
			event.setCancelled(true);
			var item = event.getItem();
			var pickupDelay = Math.max(5, item.getPickupDelay());
			item.setPickupDelay(pickupDelay);
		}
	}
}
