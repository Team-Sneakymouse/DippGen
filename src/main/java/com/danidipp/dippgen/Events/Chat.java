package com.danidipp.dippgen.Events;

import java.awt.Color;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.danidipp.dippgen.Plugin;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class Chat implements Listener {
	public static StateFlag chatFlag = new StateFlag("chat", true);

	@EventHandler(ignoreCancelled = true)
	public void chatFlag(AsyncPlayerChatEvent event) {

		Player player = event.getPlayer();
		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
		ApplicableRegionSet chatFrom = query.getApplicableRegions(localPlayer.getLocation());
		if (!chatFrom.testState(localPlayer, chatFlag)) {
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "You cannot chat here."));
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		var message = event.getMessage();
		var player = event.getPlayer();
		var baseRadius = 12d * 12d;
		var shoutRadius = 36d * 36d;

		// Remove everyone in a different world
		event.getRecipients().removeIf(p -> p.getWorld() != player.getWorld());

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

		var moderators = Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("dipp.chatspy"))
				.filter(p -> !event.getRecipients().contains(p)).toList();
		if (moderators.size() > 0) {
			var color = coordsToRGB(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
			var name = PlaceholderAPI.setPlaceholders(player, "%sneakycharacters_character_name%");
			var nameComponent = new TextComponent(name);
			nameComponent.setColor(color);
			var hoverText = ChatColor.YELLOW + "Account name: " + ChatColor.GOLD + "%player_displayname%\n" + ChatColor.YELLOW
					+ "Voicechat: %cond_voicechat-status%\n" + ChatColor.WHITE + "Teleport to player";
			nameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(PlaceholderAPI.setPlaceholders(player, hoverText))));
			nameComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + player.getName()));

			var messageComponent = new TextComponent(event.getMessage());
			messageComponent.setColor(ChatColor.DARK_GRAY);

			var text = new TextComponent();
			text.addExtra(nameComponent);
			text.addExtra(ChatColor.GRAY + ": ");
			text.addExtra(messageComponent);

			moderators.forEach(p -> p.spigot().sendMessage(text));
		}
	}

	void sendVoidAlert(Player player, Set<Player> recipients) {
		var visibleRecipients = recipients.stream().filter(p -> p.getTrackedBy().contains(player)).count();
		Plugin.plugin.getLogger().log(Level.FINE, "normal to " + recipients.size() + " players (" + visibleRecipients + " visible)");
		if (visibleRecipients <= 0)
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Nobody can hear you."));
	}

	ChatColor coordsToRGB(int x, int z) {
		int xMin = 4400;
		int xMax = 5600;
		int yMin = 4400;
		int yMax = 5600;

		double scaledX = (2 * (x - xMin) / (double) (xMax - xMin)) - 1;
		double scaledZ = (2 * (z - yMin) / (double) (yMax - yMin)) - 1;

		double hue = Math.toDegrees(Math.atan2(scaledZ, scaledX));
		hue = (hue + 360) % 360;

		double saturation = Math.sqrt(scaledX * scaledX + scaledZ * scaledZ);
		double value = 0.75;

		return ChatColor.of(new Color(Color.HSBtoRGB((float) hue / 360, (float) saturation, (float) value)));
	}
}
