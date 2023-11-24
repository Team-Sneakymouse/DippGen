package com.danidipp.lom.Commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.danidipp.lom.Plugin;

public class RegisterCommand implements ICommandImpl {
	public String getName() {
		return "register";
	};

	public CommandExecutor getExecutor() {
		return new CommandExecutor() {

			@Override
			public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
				if (!(sender instanceof Player)) {
					sender.sendMessage("Only players can use this command");
					return true;
				}
				var player = (Player) sender;

				if (args.length == 0)
					return false;

				var replacement = ((Plugin) Plugin.plugin).replacements.stream().filter(r -> r.name().equals(args[0])).findAny().orElse(null);
				if (replacement == null) {
					sender.sendMessage("Can't find replacement for \"" + args[0] + "\"");
					return true;
				}

				var target = player.getLastTwoTargetBlocks(null, 100).get(0).getLocation();
				var coordinates = target.getBlockX() + " " + target.getBlockY() + " " + target.getBlockZ();

				if (replacement.locations().stream().anyMatch(
						l -> l.getWorld() == target.getWorld() && (l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()).equals(coordinates))) {
					replacement.locations().removeIf(l -> l.getWorld() == target.getWorld()
							&& (l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()).equals(coordinates));
					((Plugin) Plugin.plugin).getConfig().set("replacements." + replacement.name(), replacement.toMap());
					((Plugin) Plugin.plugin).saveConfig();
					sender.sendMessage("Removed registration from " + coordinates);
					return true;
				}

				replacement.locations().add(target);
				((Plugin) Plugin.plugin).getConfig().set("replacements." + replacement.name(), replacement.toMap());
				((Plugin) Plugin.plugin).saveConfig();
				target.getBlock().setType(replacement.getRandomMaterial());
				sender.sendMessage("Added registration to " + coordinates);
				return true;
			}
		};
	}

	public TabCompleter getTabCompleter() {
		return new TabCompleter() {

			@Override
			public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
				if (args.length > 1)
					return new ArrayList<>();
				return ((Plugin) Plugin.plugin).replacements.stream().map(r -> r.name()).collect(Collectors.toList());
			}
		};
	}
}
