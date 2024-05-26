package com.danidipp.dippgen.Events;

import java.util.ArrayList;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.danidipp.dippgen.Plugin;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.util.HSVLike;

public class Chat implements Listener {
	public static StateFlag chatFlag = new StateFlag("chat", true);

	@EventHandler(ignoreCancelled = true)
	public void chatFlag(AsyncChatEvent event) {
		Player player = event.getPlayer();
		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
		ApplicableRegionSet chatFrom = query.getApplicableRegions(localPlayer.getLocation());
		if (!chatFrom.testState(localPlayer, chatFlag)) {
			player.sendActionBar(Component.text("You cannot chat here.", NamedTextColor.RED));
			event.setCancelled(true);
			return;
		}
	}

	enum MessageRenderType {
		NORMAL, SHOUT, GLOBAL
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void sendChatToLoki(AsyncChatEvent event) {
		var username = PlaceholderAPI.setPlaceholders(event.getPlayer(), "%sneakycharacters_character_name%");
		if (username == null || username == "%sneakycharacters_character_name%" || username.isBlank()) {
			username = event.getPlayer().getName();
		}
		var positionX = event.getPlayer().getLocation().getX();
		var positionY = event.getPlayer().getLocation().getY();
		var positionZ = event.getPlayer().getLocation().getZ();
		var message = PlainTextComponentSerializer.plainText().serialize(event.message());
		Plugin.plugin.lokiChatStream.log("{ \"username\": \"" + username + "\", \"positionX\": " + positionX + ", \"positionY\": " + positionY
				+ ", \"positionZ\": " + positionZ + ", \"message\": \"" + message + "\" }");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerChat(AsyncChatEvent event) {
		var plainText = PlainTextComponentSerializer.plainText().serialize(event.message());
		var player = event.getPlayer();
		var baseRadius = 12d * 12d;
		var shoutRadius = 36d * 36d;

		MessageRenderType renderType;

		if (plainText.length() > 2 && plainText.startsWith("!!") && player.hasPermission("dipp.chat.shout.global")) {
			// Global
			// Plugin.plugin.getLogger().log(Level.INFO, "Global message");
			event.message(componentReplaceFirst((TextComponent) event.message(), "!!", ""));
			renderType = MessageRenderType.GLOBAL;
		} else if (plainText.length() > 1 && plainText.startsWith("!") && player.hasPermission("dipp.chat.shout")) {
			// Shout
			event.message(componentReplaceFirst((TextComponent) event.message(), "!", ""));
			if (player.getHealth() > 10 || player.getGameMode() != GameMode.SURVIVAL) {
				// Large radius
				// Plugin.plugin.getLogger().log(Level.INFO, "Shout message");
				if (player.getGameMode() == GameMode.SURVIVAL)
					Bukkit.getScheduler().runTask(Plugin.plugin, () -> player.damage(10));

				event.viewers().removeIf(
						viewer -> (viewer instanceof Player) &&
								((Player) viewer).getLocation().distanceSquared(player.getLocation()) > shoutRadius);
				sendVoidAlert(player, event.viewers());
				renderType = MessageRenderType.SHOUT;
			} else {
				// Normal radius
				// Plugin.plugin.getLogger().log(Level.INFO, "Shout message (weak)");
				event.viewers().removeIf(
						viewer -> (viewer instanceof Player) &&
								((Player) viewer).getLocation().distanceSquared(player.getLocation()) > baseRadius);
				player.sendActionBar(Component.text("You are too weak to shout.", NamedTextColor.RED));
				// sendVoidAlert(player, event.getRecipients()); // No void alert because weakness alert is more important
				renderType = MessageRenderType.NORMAL;
			}
		} else {
			// Normal
			// Plugin.plugin.getLogger().log(Level.INFO, "Normal message");
			event.viewers().removeIf(viewer -> (viewer instanceof Player) &&
					((Player) viewer).getLocation().distanceSquared(player.getLocation()) > baseRadius);
			sendVoidAlert(player, event.viewers());
			renderType = MessageRenderType.NORMAL;
		}

		// Remove everyone in a different world
		if (renderType != MessageRenderType.GLOBAL) {
			event.viewers().removeIf(viewer -> (viewer instanceof Player) && ((Player) viewer).getWorld() != player.getWorld());
		}

		// Add back admins
		event.viewers().addAll(Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("dipp.chatspy")).toList());

		event.renderer((source, sourceDisplayName, message, viewer) -> {
			// TODO: OOC format
			var characterName = PlaceholderAPI.setPlaceholders(source, "%sneakycharacters_character_name%");
			sourceDisplayName = ((TextComponent) sourceDisplayName).content(characterName).color(NamedTextColor.GRAY);

			// Plugin.plugin.getLogger().log(Level.INFO, "Render final " + source.getName() + " to " + viewer.toString());
			var hoverText = Component.empty()
					.append(Component.text("Account name: ", NamedTextColor.YELLOW))
					.append(Component.text(source.getName(), NamedTextColor.GOLD))
					.append(Component.newline())
					.append(Component.text("Voicechat: ", NamedTextColor.YELLOW))
					.append(Component.text(PlaceholderAPI.setPlaceholders(source, "%cond_voicechat-status%"), NamedTextColor.GOLD));

			// Admin view
			if (viewer instanceof Player && ((Player) viewer).hasPermission("dipp.chatspy")) {
				hoverText = hoverText.append(Component.newline())
						.append(Component.text("Teleport to player", NamedTextColor.WHITE));
				sourceDisplayName = sourceDisplayName
						.clickEvent(ClickEvent.runCommand("/minecraft:tp " + source.getName()));

				var inRange = switch (renderType) {
					case GLOBAL -> true;
					case SHOUT -> source.getLocation().distanceSquared(((Player) viewer).getLocation()) <= shoutRadius;
					case NORMAL -> source.getLocation().distanceSquared(((Player) viewer).getLocation()) <= baseRadius;
				};
				if (!inRange) {
					var color = coordsToRGB(source.getLocation().getBlockX(), source.getLocation().getBlockZ());
					sourceDisplayName = sourceDisplayName.color(color);
					message = message.color(NamedTextColor.GRAY);
				}
			}
			sourceDisplayName = sourceDisplayName.hoverEvent(hoverText);

			var result = Component.empty();
			switch (renderType) {
				case GLOBAL -> result = result.append(Component.text("!! ", NamedTextColor.RED, TextDecoration.BOLD));
				case SHOUT -> result = result.append(Component.text("! ", NamedTextColor.RED, TextDecoration.BOLD));
				case NORMAL -> {
				}
			}

			return result
					.append(sourceDisplayName)
					.append(Component.text(": ", NamedTextColor.GRAY))
					.append(message);
		});
	}

	TextComponent componentReplaceFirst(TextComponent component, String search, String replace) {
		if (!component.content().isEmpty()) {
			var content = component.content().replaceFirst(search, replace);
			return component.content(content);
		} else if (!component.children().isEmpty()) {
			var children = new ArrayList<Component>(component.children());
			var child = (TextComponent) children.get(0);
			child = componentReplaceFirst(child, search, replace);
			children.set(0, child);
			return component.children(children);
		}
		return component;
	}

	void sendVoidAlert(Player player, Set<Audience> viewers) {
		var visibleRecipients = viewers.stream().filter(v -> (v instanceof Player) && ((Player) v).getTrackedBy().contains(player)).count();
		if (visibleRecipients <= 0)
			player.sendActionBar(Component.text("Nobody can hear you.", NamedTextColor.RED));
	}

	TextColor coordsToRGB(int x, int z) {
		int xMin = 4400;
		int xMax = 5600;
		int yMin = 4400;
		int yMax = 5600;

		double scaledX = (2 * (x - xMin) / (double) (xMax - xMin)) - 1;
		double scaledZ = (2 * (z - yMin) / (double) (yMax - yMin)) - 1;

		double hue = (Math.toDegrees(Math.atan2(scaledZ, scaledX)) + 360) % 360;

		double saturation = Math.hypot(scaledX, scaledZ) % 2.0;
		if (saturation > 1.0) saturation = 2.0 - saturation;

		double value = 0.75;

		var hsv = HSVLike.hsvLike((float) hue / 360, (float) saturation, (float) value);
		return TextColor.color(hsv);
	}
}
