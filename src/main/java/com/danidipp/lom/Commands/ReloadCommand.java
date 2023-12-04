package com.danidipp.lom.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.danidipp.lom.Plugin;

public class ReloadCommand implements ICommandImpl {
	public String getName() {
		return "reload";
	}

	@Override
	public CommandExecutor getExecutor() {
		return new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
				Plugin.plugin.reloadConfig();
				Plugin.plugin.parseConfig();
				return true;
			}
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return null;
	}
}
