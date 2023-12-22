package com.danidipp.dippgen.Events;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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

		var deathMessage = event.getDeathMessage();
		deathMessage = deathMessage.replaceAll("\\[playerDisplayName\\]", victim.getDisplayName());
		deathMessage = deathMessage.replaceAll("\\[sourceDisplayName\\]", killer.getDisplayName());
		if (deathMessage.contains("[item]")) {
			var item = victim.getInventory().getItemInMainHand();
			if (item != null && item.getType() != Material.AIR) {
				String itemName;
				if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					itemName = item.getItemMeta().getDisplayName();
				} else {
					itemName = item.getType().name().toLowerCase().replaceAll("_", " ");
				}

				deathMessage = deathMessage.replaceAll("\\[item\\]", itemName);
			} else {
				deathMessage = deathMessage.replaceAll("\\[item\\]", "fists");
			}
		}
		deathMessage = deathMessage.replace("was slain by", "was killed by");
		var deathText = new Text(ChatColor.translateAlternateColorCodes('&', deathMessage));
		var deathHover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, deathText);

		var killerText = new TextComponent(ChatColor.GOLD + killer.getDisplayName());
		killerText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, deathText, new Text("\nTeleport to player")));
		killerText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + killer.getName()));

		var victimText = new TextComponent(ChatColor.GOLD + victim.getDisplayName());
		victimText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, deathText, new Text("\nTeleport to player")));
		victimText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + victim.getName()));

		var locationText = new TextComponent(ChatColor.YELLOW + "[TP]");
		locationText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, deathText, new Text("\nTeleport to death position")));
		locationText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + victim.getLocation().getBlockX() + " "
				+ victim.getLocation().getBlockY() + " " + victim.getLocation().getBlockZ()));

		var text = new TextComponent(Plugin.LOG_PREFIX);
		text.setHoverEvent(deathHover);
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
