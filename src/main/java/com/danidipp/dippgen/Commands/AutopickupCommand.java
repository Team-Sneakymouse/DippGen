package com.danidipp.dippgen.Commands;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

import com.danidipp.dippgen.Plugin;

// /autopickup <player> [seconds]
public class AutopickupCommand implements ICommandImpl {
	@Override
	public String getName() {
		return "autopickup";
	}

	@Override
	public CommandExecutor getExecutor() {
		return (sender, command, label, args) -> {
			if (args.length < 1) return false;
			if (args.length == 2 && !args[1].matches("\\d+")) return false;
			var playerNames = args[0];
			var seconds = args.length > 1 ? Integer.parseInt(args[1]) : 10;
			var player = sender.getServer().getPlayer(playerNames);
			if (player == null) return false;

			var task = Bukkit.getAsyncScheduler().runDelayed(Plugin.plugin, (t) -> {
				if (Plugin.plugin.autoPickup.containsKey(player))
					Plugin.plugin.autoPickup.get(player).cancel();

				Plugin.plugin.autoPickup.remove(player);
			}, seconds, TimeUnit.SECONDS);
			Plugin.plugin.autoPickup.put(player, task);

			return true;
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return null;
	}
}
