package com.danidipp.dippgen.Events;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.danidipp.dippgen.Plugin;

import net.kyori.adventure.text.TextComponent;

public class ReplacementRegistration implements Listener {
	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		var item = event.getPlayer().getInventory().getItemInMainHand();
		if (item == null || item.getItemMeta() == null || !item.getItemMeta().hasDisplayName())
			return;
		var nameParts = ((TextComponent) item.getItemMeta().displayName()).content().split(":");
		if (nameParts.length != 2 || !nameParts[0].equals("dipp"))
			return;

		var replacement = Plugin.plugin.replacements.stream().filter(r -> r.name().equals(nameParts[1])).findAny().orElse(null);
		if (replacement == null) {
			event.getPlayer().sendMessage("Can't find replacement for \"" + nameParts[1] + "\"");
			return;
		}
		if (!event.getPlayer().hasPermission("dipp.register." + nameParts[1])) {
			event.getPlayer().sendMessage("You don't have permission to register this replacement: dipp.register." + nameParts[1]);
			return;
		}

		var target = event.getBlock().getLocation();
		var coordinates = target.getBlockX() + " " + target.getBlockY() + " " + target.getBlockZ();

		if (replacement.locations().stream().anyMatch(
				l -> l.getWorld() == target.getWorld() && (l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()).equals(coordinates))) {
			replacement.locations().removeIf(
					l -> l.getWorld() == target.getWorld() && (l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()).equals(coordinates));
			Plugin.plugin.getConfig().set("replacements." + replacement.name(), replacement.toMap());
			Plugin.plugin.saveConfig();
			target.getBlock().setType(Material.AIR);
			event.getPlayer().sendMessage("Removed registration from " + coordinates);
			event.setCancelled(true);
			return;
		} else {
			event.getPlayer().sendMessage("No registrations at location " + coordinates);
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		var item = event.getItemInHand();
		if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
			return;
		var nameParts = ((TextComponent) item.getItemMeta().displayName()).content().split(":");
		if (nameParts.length != 2 || !nameParts[0].equals("dipp"))
			return;

		var replacement = Plugin.plugin.replacements.stream().filter(r -> r.name().equals(nameParts[1])).findAny().orElse(null);
		if (replacement == null) {
			event.getPlayer().sendMessage("Can't find replacement for \"" + nameParts[1] + "\"");
			return;
		}
		if (!event.getPlayer().hasPermission("dipp.register." + nameParts[1])) {
			event.getPlayer().sendMessage("You don't have permission to register this replacement: dipp.register." + nameParts[1]);
			return;
		}

		var target = event.getBlock().getLocation();
		var coordinates = target.getBlockX() + " " + target.getBlockY() + " " + target.getBlockZ();

		if (replacement.locations().stream().anyMatch(
				l -> l.getWorld() == target.getWorld() && (l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()).equals(coordinates))) {
			event.getPlayer().sendMessage("There is already a registration at this location");
			event.setCancelled(true);
			return;
		} else {
			replacement.locations().add(target);
			Plugin.plugin.getConfig().set("replacements." + replacement.name(), replacement.toMap());
			Plugin.plugin.saveConfig();
			target.getBlock().setType(replacement.getRandomMaterial());
			event.getPlayer().sendMessage("Added " + coordinates + " as replacement");
			return;
		}
	}
}
