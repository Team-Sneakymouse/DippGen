package com.danidipp.dippgen.Events;

import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.danidipp.dippgen.Plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class Shout implements Listener {
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		var message = event.getMessage();
		var player = event.getPlayer();
		var baseRadius = 12d * 12d;
		var shoutRadius = 36d * 36d;

		if (message.length() > 2 && message.startsWith("!!") && player.hasPermission("dipp.chat.shout.global")) {
			// Send to everyone
			event.setMessage(message.substring(2));
			event.setFormat(ChatColor.RED + "" + ChatColor.BOLD + "!! " + ChatColor.RESET + event.getFormat());
		} else if (message.length() > 2 && message.startsWith("!") && player.hasPermission("dipp.chat.shout")) {
			if (player.getHealth() > 10 || player.getGameMode() != GameMode.SURVIVAL) {
				// Large radius
				if (player.getGameMode() == GameMode.SURVIVAL)
					Bukkit.getScheduler().runTask(Plugin.plugin, () -> player.damage(10));
				event.setMessage(message.substring(1));
				event.setFormat(ChatColor.RED + "" + ChatColor.BOLD + "! " + ChatColor.RESET + event.getFormat());
				event.getRecipients().removeIf(p -> p.getLocation().distanceSquared(player.getLocation()) > shoutRadius);
				sendVoidAlert(player, event.getRecipients());
			} else {
				// Normal radius
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "You are too weak to shout."));
				event.setMessage(message.substring(1));
				event.getRecipients().removeIf(p -> p.getLocation().distanceSquared(player.getLocation()) > baseRadius);
				// sendVoidAlert(player, event.getRecipients()); // No void alert because weakness alert is more important
			}
		} else {
			// Normal radius
			event.getRecipients().removeIf(p -> p.getLocation().distanceSquared(player.getLocation()) > baseRadius);
			sendVoidAlert(player, event.getRecipients());
		}
	}

	void sendVoidAlert(Player player, Set<Player> recipients) {
		var visibleRecipients = recipients.stream().filter(p -> p.getTrackedBy().contains(player)).count();
		Plugin.plugin.getLogger().log(Level.INFO, "normal to " + recipients.size() + " players (" + visibleRecipients + " visible)");
		if (visibleRecipients <= 0)
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Nobody can hear you."));
	}
}
