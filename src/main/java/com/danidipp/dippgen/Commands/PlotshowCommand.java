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
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
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
			if (args.length == 0) return false; // No player name provided

			String playerName = args[0];
			OfflinePlayer player;
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

			// Get all regions
			World world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
			var wgWorld = BukkitAdapter.adapt(world);
			RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(wgWorld);
			Collection<ProtectedRegion> regions = regionManager != null ? regionManager.getRegions().values() : List.of();

			// Filter regions by owner
			var uuid = player.getUniqueId();
			var ownerRegions = regions.stream().filter(r -> r.getOwners().contains(uuid)).toList();
			var memberRegions = regions.stream().filter(r -> r.getMembers().contains(uuid)).toList();

			// Display regions
			sender.sendMessage(Plugin.LOG_PREFIX.append(Component.text(playerName + " owns " + ownerRegions.size() + " plots:")));
			for (var region : ownerRegions) {
				var text = formatRegion(region, world);
				sender.sendMessage(text);
			}

			if (memberRegions.size() > 0) {
				sender.sendMessage(Plugin.LOG_PREFIX.append(Component.text(playerName + " is a member of " + memberRegions.size() + " plots:")));
				for (var region : memberRegions) {
					var text = formatRegion(region, world);
					sender.sendMessage(text);
				}
			}

			return true;
		};
	}

	Component formatRegion(ProtectedRegion region, World world) {
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

		var text = Component.textOfChildren(Component.text("- " + region.getId() + " "), infoComponent, Component.space(), teleportComponent,
				Component.space(), chestsComponent);

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
