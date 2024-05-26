package com.danidipp.dippgen.Events;

import org.bukkit.Bukkit;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.danidipp.dippgen.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class RDMMonitor implements Listener {
	public boolean limitPlayerDamage(Player player) {
		if (player.hasPermission("dipp.rdmbypass")) return false;
		var playtimeString = PlaceholderAPI.setPlaceholders(player, "%cmi_user_playtime_hourst%");
		try {
			var playtime = Float.parseFloat(playtimeString);
			if (playtime < 100.0) return true;
		} catch (NumberFormatException e) {
			Plugin.plugin.getLogger().warning("Error parsing playtime: " + playtimeString);
		}
		return false;
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageByEntityEvent event) {
		var damager = event.getDamager();
		var victim = event.getEntity();

		if (!(damager instanceof Player) || !(victim instanceof Player)) return;
		if (limitPlayerDamage((Player) damager)) event.setCancelled(true);
	}

	@EventHandler
	public void onBobberHit(ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof FishHook)) return;
		if (event.getHitEntity() == null || !(event.getHitEntity() instanceof Player)) return;
		var hooker = event.getEntity().getShooter();
		if (hooker == null || !(hooker instanceof Player)) return;

		if (limitPlayerDamage((Player) hooker)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onKill(PlayerDeathEvent event) {
		var killer = event.getEntity().getKiller();
		if (killer == null || !(killer instanceof Player)) return;

		var victim = event.getEntity();

		TextComponent deathMessage = (TextComponent) event.deathMessage();

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

		var text = Plugin.LOG_PREFIX
				.hoverEvent(HoverEvent.showText(deathMessage))
				.append(killerText)
				.append(Component.text(" killed ", NamedTextColor.YELLOW))
				.append(victimText)
				.append(Component.space())
				.append(locationText);

		for (var player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.isOp() || player.hasPermission("dipp.rdmspy")) {
				player.sendMessage(text);
			}
		}
	}

}
