package com.danidipp.dippgen.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.danidipp.dippgen.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class JoinSpy implements Listener {

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String message = null;
		if (!player.hasPlayedBefore()) {
			message = " joined the server for the first time! ";
		} else {
			String playtime = PlaceholderAPI.setPlaceholders(player, "%cmi_user_playtime_hourst%");
			try {
				double hours = Double.parseDouble(playtime);
				if (hours < 2) {
					message = " joined the server with " + playtime + " hours of playtime. ";
				}
			} catch (NumberFormatException e) {
				Plugin.plugin.getLogger().warning("Could not parse playtime for " + player.getName() + ": " + playtime);
			}
		}

		if (message == null) {
			return;
		}
		var playerComponent = new TextComponent(ChatColor.YELLOW + player.getDisplayName() + ChatColor.RESET);
		playerComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Teleport to player")));
		playerComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + player.getName()));

		var infoComponent = new TextComponent(ChatColor.YELLOW + "[info]");
		infoComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Show player info")));
		infoComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cmi info " + player.getName()));

		var altComponent = new TextComponent(ChatColor.YELLOW + "[alts]");
		altComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Show alts accounts")));
		altComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cmi checkaccount " + player.getName()));

		var text = new TextComponent(Plugin.LOG_PREFIX);
		text.addExtra("New player ");
		text.addExtra(playerComponent);
		text.addExtra(message);
		text.addExtra(infoComponent);
		text.addExtra(" ");
		text.addExtra(altComponent);

		for (Player p : Plugin.plugin.getServer().getOnlinePlayers()) {
			if (p.hasPermission("dipp.joinspy")) {
				p.spigot().sendMessage(text);
			}
		}
	}

}
