package com.danidipp.dippgen.Commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

public interface ICommandImpl {
	public String getName();

	public CommandExecutor getExecutor();

	public TabCompleter getTabCompleter();
}
