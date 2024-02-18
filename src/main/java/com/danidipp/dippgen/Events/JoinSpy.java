package com.danidipp.dippgen.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.danidipp.dippgen.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

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
		var playerComponent = player.displayName().color(NamedTextColor.YELLOW);
		playerComponent = playerComponent.hoverEvent(HoverEvent.showText(Component.text("Teleport to player")));
		playerComponent = playerComponent.clickEvent(ClickEvent.runCommand(("/minecraft:tp " + player.getName())));

		var infoComponent = Component.text("[info]", NamedTextColor.YELLOW);
		infoComponent = infoComponent.hoverEvent(HoverEvent.showText(Component.text("Show player info")));
		infoComponent = infoComponent.clickEvent(ClickEvent.runCommand("/cmi info " + player.getName()));

		var altComponent = Component.text("[alts]", NamedTextColor.YELLOW);
		altComponent = altComponent.hoverEvent(HoverEvent.showText(Component.text("Show alts accounts")));
		altComponent = altComponent.clickEvent(ClickEvent.runCommand("/cmi checkaccount " + player.getName()));

		var text = Plugin.LOG_PREFIX
				.append(Component.text("New player "))
				.append(playerComponent)
				.append(Component.text(message))
				.append(infoComponent)
				.append(Component.space())
				.append(altComponent);

		for (Player p : Plugin.plugin.getServer().getOnlinePlayers()) {
			if (p.hasPermission("dipp.joinspy")) {
				p.sendMessage(text);
			}
		}
	}

}
