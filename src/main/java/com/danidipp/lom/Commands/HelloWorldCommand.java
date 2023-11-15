package com.danidipp.lom.Commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class HelloWorldCommand implements ICommandImpl {
	public String getName() {
		return "helloworld";
	};

	public CommandExecutor getExecutor() {
		return new CommandExecutor() {

			@Override
			public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
				sender.sendMessage("Hello, World!");
				return true;
			}
		};
	}

	public TabCompleter getTabCompleter() {
		return new TabCompleter() {

			@Override
			public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
				ArrayList<String> list = new ArrayList<String>();
				list.add("hello");
				return list;
			}
		};
	}
}
