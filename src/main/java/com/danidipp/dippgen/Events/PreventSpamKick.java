package com.danidipp.dippgen.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class PreventSpamKick implements Listener {
	@EventHandler
	public void onKick(PlayerKickEvent event) {
		if (event.getPlayer().hasPermission("dipp.spambypass") && event.getReason().equals("Kicked for spamming")) {
			event.setCancelled(true);
		}
	}
}
