package com.danidipp.dippgen.Events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.danidipp.dippgen.Plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class RDMMonitor implements Listener {
	@EventHandler
	public void onKill(PlayerDeathEvent event) {
		var killer = event.getEntity().getKiller();
		if (killer == null || !(killer instanceof Player))
			return;

		var victim = event.getEntity();

		TextComponent deathMessage = (TextComponent) event.deathMessage();
		// deathMessage = deathMessage.replaceAll("\\[playerDisplayName\\]", victim.getDisplayName());
		// deathMessage = deathMessage.replaceAll("\\[sourceDisplayName\\]", killer.getDisplayName());
		// if (deathMessage.contains("[item]")) {
		// 	var item = victim.getInventory().getItemInMainHand();
		// 	if (item != null && item.getType() != Material.AIR) {
		// 		String itemName;
		// 		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
		// 			itemName = item.getItemMeta().getDisplayName();
		// 		} else {
		// 			itemName = item.getType().name().toLowerCase().replaceAll("_", " ");
		// 		}

		// 		deathMessage = deathMessage.replaceAll("\\[item\\]", itemName);
		// 	} else {
		// 		deathMessage = deathMessage.replaceAll("\\[item\\]", "fists");
		// 	}
		// }

		var killerText = killer.displayName().color(NamedTextColor.GOLD);
		killerText.hoverEvent(HoverEvent.showText(deathMessage.append(Component.newline()).append(Component.text("Teleport to player"))));
		killerText.clickEvent(ClickEvent.runCommand("/minecraft:tp " + killer.getName()));

		var victimText = victim.displayName().color(NamedTextColor.GOLD);
		victimText.hoverEvent(HoverEvent.showText(deathMessage.append(Component.newline()).append(Component.text("Teleport to player"))));
		victimText.clickEvent(ClickEvent.runCommand("/minecraft:tp " + victim.getName()));

		var locationText = Component.text("[TP]", NamedTextColor.YELLOW);
		locationText.hoverEvent(deathMessage.append(Component.newline()).append(Component.text("Teleport to death position")));
		locationText.clickEvent(ClickEvent.runCommand("/minecraft:tp " + victim.getLocation().getBlockX() + " " + victim.getLocation().getBlockY()
				+ " " + victim.getLocation().getBlockZ()));

		var text = Plugin.LOG_PREFIX;
		text.hoverEvent(HoverEvent.showText(deathMessage));
		text.append(killerText);
		text.append(Component.text(" killed ", NamedTextColor.YELLOW));
		text.append(victimText);
		text.append(Component.space());
		text.append(locationText);

		for (var player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.isOp() || player.hasPermission("dipp.rdmspy")) {
				player.sendMessage(text);
			}
		}
	}

}
