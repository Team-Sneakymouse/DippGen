package com.danidipp.dippgen.Commands;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.danidipp.dippgen.Plugin;
import com.danidipp.dippgen.Modules.PlotManagement.Plot;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

// Displays all plots owned by a player
public class PlotshowCommand implements ICommandImpl {
	public String getName() {
		return "plotshow";
	}

	@Override
	public CommandExecutor getExecutor() {
		return (sender, command, label, args) -> {
			OfflinePlayer player;
			if (args.length == 0) {
				if (!(sender instanceof Player)) {
					sender.sendMessage("error: You must be on the server to use this command without specifying a player");
					return true;
				}
				var plot = Plot.getPlot(((Player) sender).getLocation());
				if (plot == null) {
					sender.sendMessage("error: You are not in a plot");
					return true;
				}
				var uuid = plot.region().getOwners().getUniqueIds().stream().findFirst().orElse(null);
				if (uuid == null) {
					sender.sendMessage("error: Plot has no owner");
					return true;
				}
				player = Bukkit.getOfflinePlayer(uuid);
			} else {
				String playerName = args[0];
				try {
					var uuid = UUID.fromString(playerName);
					player = Bukkit.getOfflinePlayer(uuid);
				} catch (IllegalArgumentException e) {
					player = List.of(Bukkit.getOfflinePlayers()).stream().filter(p -> p.getName().toLowerCase().equals(playerName.toLowerCase()))
							.findFirst().orElse(null);
				}

				if (player == null) {
					sender.sendMessage("error: Player " + playerName + " does not exist");
					return true;
				}
			}

			// Get all regions
			World world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
			var wgWorld = BukkitAdapter.adapt(world);
			RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wgWorld);
			Collection<ProtectedRegion> regions = regionManager != null ? regionManager.getRegions().values() : List.of();

			// Filter regions by owner
			var wgPlayer = WorldGuardPlugin.inst().wrapOfflinePlayer(player);
			var ownerRegions = regions.stream().filter(r -> r.getOwners().contains(wgPlayer)).toList();
			var memberRegions = regions.stream().filter(r -> r.getMembers().contains(wgPlayer)).toList();

			// Display player info
			var infoComponent = Component.text("[info]", NamedTextColor.YELLOW)
					.hoverEvent(HoverEvent.showText(Component.text("Show player info")))
					.clickEvent(ClickEvent.runCommand("/cmi info " + player.getName()));

			var altsComponent = Component.text("[alts]", NamedTextColor.YELLOW)
					.hoverEvent(HoverEvent.showText(Component.text("Show player alts")))
					.clickEvent(ClickEvent.runCommand("/cmi checkaccount " + player.getName()));

			var isOnline = player.isOnline();
			var tpCommand = "/minecraft:tp @s " + player.getName();
			var teleportComponent = Component.text("[tp]", isOnline ? NamedTextColor.YELLOW : NamedTextColor.GRAY)
					.hoverEvent(HoverEvent.showText(Component.text(isOnline ? "Teleport to player" : "Player is offline")))
					.clickEvent(isOnline ? ClickEvent.runCommand(tpCommand) : null);

			sender.sendMessage(Component.textOfChildren(
					Plugin.LOG_PREFIX, Component.text("Plot info for ", NamedTextColor.GRAY),
					Component.text(player.getName(), NamedTextColor.WHITE).appendSpace(),
					infoComponent.appendSpace(),
					altsComponent.appendSpace(),
					teleportComponent));
			// Display regions
			sender.sendMessage(Component.text(player.getName() + " owns " + ownerRegions.size() + " plots:", NamedTextColor.GRAY));
			for (var region : ownerRegions) {
				var indirect = !region.getOwners().getPlayerDomain().contains(wgPlayer);
				sender.sendMessage(formatRegion(region, world, indirect));
			}

			if (memberRegions.size() > 0) {
				sender.sendMessage(Component.text(player.getName() + " is a member of " + memberRegions.size() + " plots:", NamedTextColor.GRAY));
				for (var region : memberRegions) {
					var indirect = !region.getMembers().contains(wgPlayer);
					sender.sendMessage(formatRegion(region, world, indirect, true));
				}
			}

			return true;
		};
	}

	Component formatRegion(ProtectedRegion region, World world) {
		return formatRegion(region, world, false, false);
	}

	Component formatRegion(ProtectedRegion region, World world, boolean indirect) {
		return formatRegion(region, world, indirect, false);
	}

	Component formatRegion(ProtectedRegion region, World world, boolean indirect, boolean showOwner) {
		var regionCenter = region.getMinimumPoint().add(region.getMaximumPoint()).divide(2);
		var centerY = region.getMaximumPoint().getBlockY();
		while (world.getBlockAt(regionCenter.getBlockX(), centerY, regionCenter.getBlockZ()).isPassable()) { centerY--; }
		regionCenter = regionCenter.withY(centerY + 1);

		var infoComponent = Component.text("[info]", NamedTextColor.YELLOW)
				.hoverEvent(HoverEvent.showText(Component.text("Show plot info")))
				.clickEvent(ClickEvent.runCommand("/rg info -w " + world.getName() + " " + region.getId()));

		var tpCommand = "/minecraft:tp @s " + regionCenter.getBlockX() + " " + regionCenter.getBlockY() + " " + regionCenter.getBlockZ();
		var teleportComponent = Component.text("[tp]", NamedTextColor.YELLOW)
				.hoverEvent(HoverEvent.showText(Component.text("Teleport to plot")))
				.clickEvent(ClickEvent.runCommand(tpCommand));

		var hasContainers = PlotChestsCommand.getContainers(region, world).size() > 0;
		var chestsComponent = Component.text("[chests]")
				.color(hasContainers ? NamedTextColor.YELLOW : NamedTextColor.GRAY)
				.hoverEvent(HoverEvent.showText(Component.text(hasContainers ? "Show plot chests" : "No chests found")))
				.clickEvent(hasContainers ? ClickEvent.runCommand("/dippgen:plotchests " + world.getName() + ":" + region.getId()) : null);

		var ownerName = region.getOwners().getUniqueIds().stream().map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).findFirst()
				.orElse("Unknown");
		var regionName = Component.text(region.getId(), NamedTextColor.WHITE);
		if (showOwner) {
			regionName = Component.text(ownerName + "'s plot", NamedTextColor.WHITE)
					.hoverEvent(HoverEvent.showText(Component.text(region.getId())));
		}
		if (indirect) {
			regionName = regionName.color(NamedTextColor.GRAY).hoverEvent(HoverEvent.showText(Component.text("via group")));
		}
		var text = Component.textOfChildren(
				Component.text("- "),
				regionName, Component.space(),
				infoComponent, Component.space(),
				teleportComponent, Component.space(),
				chestsComponent);

		return text;
	}

	@Override
	public TabCompleter getTabCompleter() {
		return new TabCompleter() {
			@Override
			public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
				var playerlist = List.of(Bukkit.getOfflinePlayers()).stream().filter(p -> p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
						.map(p -> p.getName()).collect(Collectors.toList());
				return playerlist;
			}
		};
	}
}
