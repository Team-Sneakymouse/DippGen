package com.danidipp.dippgen.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

// /opencontainer <world> <x> <y> <z>
public class OpenContainerCommand implements ICommandImpl {
	@Override
	public String getName() {
		return "opencontainer";
	}

	@Override
	public CommandExecutor getExecutor() {
		return (sender, command, label, args) -> {
			if (args.length != 4) {
				return false;
			}
			if (!(sender instanceof org.bukkit.entity.Player)) {
				sender.sendMessage("error: Only players can use this command");
				return true;
			}
			var player = (org.bukkit.entity.Player) sender;

			var world = Bukkit.getWorld(args[0]);
			if (world == null) {
				player.sendMessage("error: World " + args[0] + " does not exist");
				return true;
			}

			int x, y, z;
			try {
				x = Integer.parseInt(args[1]);
				y = Integer.parseInt(args[2]);
				z = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				player.sendMessage("error: Invalid coordinates");
				return true;
			}

			var block = world.getBlockAt(x, y, z);
			if (!(block.getState() instanceof org.bukkit.block.Container)) {
				player.sendMessage("error: Block is not a container");
				return true;
			}

			var container = (org.bukkit.block.Container) block.getState();
			player.openInventory(container.getInventory());
			return true;
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return (sender, command, alias, args) -> {
			return null;
		};
	}
}
