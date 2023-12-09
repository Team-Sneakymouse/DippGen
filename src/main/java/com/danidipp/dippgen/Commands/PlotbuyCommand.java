package com.danidipp.dippgen.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.checkerframework.checker.units.qual.radians;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.config.WorldConfiguration;
import com.sk89q.worldguard.protection.managers.RegionManager;
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

				if (atPlotLimit(wgPlayer)) {
					Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + playerName + " error-PlotOwned");
					return true;
				}

				ProtectedRegion plotRegion = regions.stream().filter(r -> r.getPriority() == 5).findFirst().orElse(null);
				if (plotRegion == null) {
					return true;
				}

				if (plotRegion.getOwners().size() > 0) {
					Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + playerName + " error-PlotOwned");
					return true;
				}

				plotRegion.getOwners().addPlayer(wgPlayer);
				Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), "cast forcecast " + playerName + " success-PlotBuy");

				return true;
			}
		};
	}

	boolean atPlotLimit(LocalPlayer wgPlayer) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		WorldConfiguration wcfg = WorldGuard.getInstance().getPlatform().getGlobalStateManager().get(wgPlayer.getWorld());
		RegionManager manager = container.get(wgPlayer.getWorld());
		var maxRegionCount = wcfg.getMaxRegionCount(wgPlayer);
		var ownedRegions = manager.getRegionCountOfPlayer(wgPlayer);

		return maxRegionCount >= 0 && ownedRegions >= maxRegionCount;
	}

	@Override
	public TabCompleter getTabCompleter() {
		return null;
	}
}
