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

public class PlotsellCommand implements ICommandImpl {
	public String getName() {
		return "plotsell";
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
					return true;
				}

				if (!plotRegion.getOwners().contains(wgPlayer)) {
					Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + playerName + " error-PlotNotOwned");
					return true;
				}

				plotRegion.getOwners().removePlayer(wgPlayer);
				Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + playerName + " success-PlotSell");
				return true;
			}
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return null;
	}
}
