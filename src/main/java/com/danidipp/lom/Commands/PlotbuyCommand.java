package com.danidipp.lom.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public class PlotbuyCommand implements ICommandImpl {
	public String getName() {
		return "plotbuy";
	}

	@Override
	public CommandExecutor getExecutor() {
		return new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
				if (args.length == 0)
					return false;
				var playerName = args[0];
				var player = Bukkit.getPlayer(playerName);

				if (player == null) {
					sender.sendMessage("error: Player " + playerName + " is not online");
					return true;
				}
				var wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

				RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
				RegionQuery query = container.createQuery();
				var regions = query.getApplicableRegions(wgPlayer.getLocation()).getRegions();

				ProtectedRegion plotRegion = regions.stream().filter(r -> r.getPriority() == 5).findFirst().orElse(null);
				if (plotRegion == null) {
					// sender.sendMessage("error: Not in any plot");
					return true;
				}

				if (plotRegion.getOwners().size() > 0) {
					// sender.sendMessage("error: Plot is already owned");
					player.chat("/cast error-PlotOwned");
					return true;
				}

				plotRegion.getOwners().addPlayer(wgPlayer);
				return true;
			}
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return null;
	}
}
