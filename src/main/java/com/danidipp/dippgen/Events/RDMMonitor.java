package com.danidipp.dippgen.Events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.danidipp.dippgen.Plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class RDMMonitor implements Listener {
	@EventHandler
	public void onKill(PlayerDeathEvent event) {
		var killer = event.getEntity().getKiller();
		if (killer == null || !(killer instanceof Player))
			return;

		var victim = event.getEntity();

		var killerText = new TextComponent(ChatColor.GOLD + killer.getDisplayName());
		killerText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Teleport to current position")));
		killerText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + killer.getName()));

		var victimText = new TextComponent(ChatColor.GOLD + victim.getDisplayName());
		victimText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Teleport to current position")));
		victimText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + victim.getName()));

		var locationText = new TextComponent(ChatColor.YELLOW + "[TP]");
		locationText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Teleport to death position")));
		locationText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + killer.getName() + " "
				+ victim.getLocation().getBlockX() + " " + victim.getLocation().getBlockY() + " " + victim.getLocation().getBlockZ()));

		var text = new TextComponent();
		text.addExtra(Plugin.LOG_PREFIX);
		text.addExtra(killerText);
		text.addExtra(ChatColor.YELLOW + " killed ");
		text.addExtra(victimText);
		text.addExtra(" ");
		text.addExtra(locationText);

		for (var player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.isOp() || player.hasPermission("dipp.rdmspy")) {
				player.spigot().sendMessage(text);
			}
		}
	}

}
