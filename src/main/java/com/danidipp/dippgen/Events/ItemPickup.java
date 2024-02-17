package com.danidipp.dippgen.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

public class ItemPickup implements Listener {
	@EventHandler
	public void onItemPickup(PlayerAttemptPickupItemEvent event) {
		if (!event.getPlayer().isSneaking()) {
			event.setCancelled(true);
			var item = event.getItem();
			var pickupDelay = Math.max(5, item.getPickupDelay());
			item.setPickupDelay(pickupDelay);
		}
	}
}
