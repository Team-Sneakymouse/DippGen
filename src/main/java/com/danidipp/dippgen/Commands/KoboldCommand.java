package com.danidipp.dippgen.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.danidipp.dippgen.Plugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class KoboldCommand implements ICommandImpl {
	private static Map<Player, ScheduledTask> koboldPlayers = new HashMap<>();

	@Override
	public String getName() {
		return "kobold";
	}

	@Override
	public CommandExecutor getExecutor() {
		return (sender, command, label, args) -> {
			if (args.length == 0) return false;
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be used by a player.");
				return true;
			}
			var player = (Player) sender;
			if (koboldPlayers.containsKey(player)) {
				var task = koboldPlayers.get(player);
				task.cancel();
				koboldPlayers.remove(player);
			}
			var task = Bukkit.getAsyncScheduler().runDelayed(Plugin.plugin, (t) -> koboldPlayers.remove(player), 1, TimeUnit.SECONDS);
			koboldPlayers.put(player, task);

			player.chat(String.join(" ", args));
			return true;
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return (sender, command, alias, args) -> {
			return null;
		};
	}

	public static Listener listener = new Listener() {
		@EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
		public void onPlayerChat(AsyncChatEvent event) {
			var player = event.getPlayer();
			if (!koboldPlayers.containsKey(player)) return;

			koboldPlayers.get(player).cancel();
			koboldPlayers.remove(player);

			var plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
			var koboldMessage = plainMessage
					.replaceAll("[\\w']+", "kobold")
					.replaceAll("\\. k", ". K")
					.replaceFirst("^k", "K");

			var renderer = event.renderer();
			event.renderer((player1, displayName, message, viewer) -> {
				if (viewer instanceof Player) {
					var characterTags = PlaceholderAPI.setPlaceholders((Player) viewer, "%sneakycharacters_character_tags%").split(",");
					if (!Arrays.stream(characterTags).anyMatch("kobold"::equals)) {
						message = Component.text(koboldMessage);
					}
				}
				return renderer.render(player1, displayName, message, viewer);
			});
		}
	};
}
