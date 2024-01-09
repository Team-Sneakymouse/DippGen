package com.danidipp.dippgen.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.danidipp.dippgen.Plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class JoinSpy implements Listener {

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.hasPlayedBefore()) {
			return;
		}
		Plugin.plugin.getLogger().info("New player " + player.getDisplayName() + " joined the server for the first time!");

		var message = new TextComponent(Plugin.LOG_PREFIX);
		message.addExtra("New player ");
		message.addExtra(ChatColor.YELLOW + player.getDisplayName() + ChatColor.RESET);
		message.addExtra(" joined the server for the first time!");
		message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + player.getName()));
		message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Teleport to player")));

		Plugin.plugin.getServer().broadcast(message.toLegacyText(), "dipp.joinspy");
	}

}
