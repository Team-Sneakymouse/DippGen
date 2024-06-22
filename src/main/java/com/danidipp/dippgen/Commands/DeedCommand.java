package com.danidipp.dippgen.Commands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.danidipp.dippgen.Modules.PlotManagement.Plot;
import com.danidipp.dippgen.Modules.PlotManagement.PlotDeed;

public class DeedCommand implements ICommandImpl {

	@Override
	public String getName() {
		return "deed";
	}

	@Override
	public CommandExecutor getExecutor() {
		return (sender, command, label, args) -> {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Only players can use this command");
				return true;
			}
			var player = (Player) sender;
			var plot = Plot.getPlot(player.getLocation());
			if (plot == null) {
				sender.sendMessage("You are not in a plot");
				return true;
			}
			if (!plot.isManager(player) && !player.hasPermission("dipp.admin")) {
				sender.sendMessage("You are not the owner of this plot");
				return true;
			}
			var deedItem = PlotDeed.getDeedItem(plot, PlotDeed.DEED_TYPE.MANAGEMENT);
			if (deedItem == null) {
				sender.sendMessage("Something went wrong. Please tell Dani! (" + plot.getId() + ")");
				return true;
			}
			player.getInventory().addItem(deedItem);
			return true;
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return null;
	}

}
