package com.danidipp.dippgen.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class PreventSpamKick implements Listener {
	@EventHandler
	public void onKick(PlayerKickEvent event) {
		var plainTextReason = PlainTextComponentSerializer.plainText().serialize(event.reason());
		if (event.getPlayer().hasPermission("dipp.spambypass") && plainTextReason.equals("Kicked for spamming")) {
			event.setCancelled(true);
		}
	}
}
