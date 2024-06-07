package com.danidipp.dippgen.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

import com.danidipp.dippgen.Plugin;
import com.danidipp.dippgen.Modules.PlotManagement.Plot;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlotChestsCommand implements ICommandImpl {

	@Override
	public String getName() {
		return "plotchests";
	}

	@Override
	public CommandExecutor getExecutor() {
		return (sender, command, label, args) -> {
			if (args.length != 1) return false;
			var plotId = args[0];
			var plot = Plot.getPlot(plotId);
			if (plot == null) {
				sender.sendMessage("error: Plot " + plotId + " does not exist");
				return true;
			}

			var inventoryBlocks = getContainers(plot.region(), plot.world());

			sender.sendMessage(Plugin.LOG_PREFIX.append(Component.text("Found " + inventoryBlocks.size() + " inventories in plot " + plotId + ":")));
			for (var inventoryBlock : inventoryBlocks) { sender.sendMessage(formatBlock(inventoryBlock)); }

			return true;
		};
	}

	public static Set<Block> getContainers(ProtectedRegion region, World world) {
		Region weRegion;

		if (region instanceof ProtectedCuboidRegion) {
			var min = ((ProtectedCuboidRegion) region).getMinimumPoint();
			var max = ((ProtectedCuboidRegion) region).getMaximumPoint();

			weRegion = new com.sk89q.worldedit.regions.CuboidRegion(BukkitAdapter.adapt(world), min, max);
		} else if (region instanceof ProtectedPolygonalRegion) {
			var points = ((ProtectedPolygonalRegion) region).getPoints();
			var minY = ((ProtectedPolygonalRegion) region).getMinimumPoint().getBlockY();
			var maxY = ((ProtectedPolygonalRegion) region).getMaximumPoint().getBlockY();

			weRegion = new com.sk89q.worldedit.regions.Polygonal2DRegion(BukkitAdapter.adapt(world), points, minY, maxY);
		} else {
			throw new RuntimeException("Unsupported region type: " + region.getType().name());
		}

		Set<Block> inventoryBlocks = new HashSet<>();
		for (var vec3 : weRegion) {
			var block = world.getBlockAt(vec3.getBlockX(), vec3.getBlockY(), vec3.getBlockZ());
			if (block.getState() instanceof Container) {
				inventoryBlocks.add(block);
			}
		}
		return inventoryBlocks;
	}

	static Component formatBlock(Block inventoryBlock) {
		Container container = (Container) inventoryBlock.getState();
		var name = container.customName() != null ? container.customName() : Component.text(inventoryBlock.getType().name());
		var pos = inventoryBlock.getLocation();
		var slotsUsed = Arrays.stream(container.getInventory().getContents()).filter(item -> item != null).count();
		var maxSlots = container.getInventory().getSize();

		var containerCommand = "/dippgen:opencontainer " + pos.getWorld().getName() + " " + pos.getBlockX() + " " + pos.getBlockY() + " "
				+ pos.getBlockZ();
		TextComponent text = Component.textOfChildren(name, Component.space(), Component.text("[", NamedTextColor.DARK_GRAY),
				Component.text(pos.getBlockX(), NamedTextColor.GRAY), Component.text(", ", NamedTextColor.DARK_GRAY),
				Component.text(pos.getBlockY(), NamedTextColor.GRAY), Component.text(", ", NamedTextColor.DARK_GRAY),
				Component.text(pos.getBlockZ(), NamedTextColor.GRAY), Component.text("]: ", NamedTextColor.DARK_GRAY),
				Component.text(slotsUsed, NamedTextColor.YELLOW), Component.text("/", NamedTextColor.WHITE),
				Component.text(maxSlots, NamedTextColor.YELLOW))
				.hoverEvent(HoverEvent.showText(Component.text("Click to open")))
				.clickEvent(ClickEvent.runCommand(containerCommand));
		return text;
	}

	@Override
	public TabCompleter getTabCompleter() {
		return (sender, command, alias, args) -> {
			if (args.length != 1) {
				return List.of();
			}
			var worlds = Bukkit.getWorlds();
			if (args[0].contains(":")) {
				var worldName = args[0].split(":")[0];
				if (worlds.stream().anyMatch(world -> world.getName().equals(worldName))) {
					worlds = List.of(Bukkit.getWorld(worldName));
				}
			}

			var regionNames = new ArrayList<String>();
			for (var world : worlds) {
				var wgWorld = BukkitAdapter.adapt(world);
				RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
				var regionManager = container.get(wgWorld);
				if (regionManager == null)
					continue;
				regionManager.getRegions().keySet().forEach(regionName -> regionNames.add(world.getName() + ":" + regionName));
			}
			return regionNames.stream().filter(regionName -> regionName.startsWith(args[0])).collect(Collectors.toList());
		};
	}

}
