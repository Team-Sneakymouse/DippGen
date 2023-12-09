package com.danidipp.dippgen.Events;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class CheckDistrictOnBlockPlace implements Listener {

	@EventHandler
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("dipp.admin"))
			return;

		var regions = getRegions(event.getBlock().getLocation());
		var topRegionIsolatePermission = "dipp." + regions.getFirst().getId() + ".isolate";
		if (regions.getFirst().getPriority() != 5 && !player.hasPermission(topRegionIsolatePermission)) {
			// Player is not in a plot
			if (player.hasPermission("dipp.debug")) {
				var error = new TextComponent("Region " + regions.getFirst().getId() + " is not isolated.");
				error.setColor(ChatColor.RED);
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, error);
			}
			event.setCancelled(true);
			return;
		}

		for (var region : regions) {
			var permission = "dipp." + region.getId() + ".place." + event.getBlockPlaced().getType().name().toLowerCase();
			if (player.hasPermission(permission)) { // allow event to go through
				if (player.hasPermission("dipp.debug"))
					player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(permission));
				return;
			}

			var isolatedPermission = "dipp." + region.getId() + ".isolate";
			if (player.hasPermission(isolatedPermission)) {
				break;
			}
		}

		if (player.hasPermission("dipp.debug")) {
			var regionNames = regions.stream().map(r -> r.getId()).collect(Collectors.joining(", "));
			var error = new TextComponent("No permission to place " + event.getBlockPlaced().getType().name() + " in " + regionNames);
			error.setColor(ChatColor.RED);
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, error);
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("dipp.admin"))
			return;

		var regions = getRegions(event.getBlock().getLocation());
		var topRegionIsolatePermission = "dipp." + regions.getFirst().getId() + ".isolate";
		if (regions.getFirst().getPriority() != 5 && !player.hasPermission(topRegionIsolatePermission)) {
			// Player is not in a plot
			if (player.hasPermission("dipp.debug")) {
				var error = new TextComponent("Region " + regions.getFirst().getId() + " is not isolated.");
				error.setColor(ChatColor.RED);
				player.spigot().sendMessage(ChatMessageType.ACTION_BAR, error);
			}
			event.setCancelled(true);
			return;
		}

		for (var region : regions) {
			var permission = "dipp." + region.getId() + ".break." + event.getBlock().getType().name().toLowerCase();
			if (player.hasPermission(permission)) { // allow event to go through
				return;
			}

			var isolatedPermission = "dipp." + region.getId() + ".isolate";
			if (player.hasPermission(isolatedPermission)) {
				break;
			}
		}
		if (player.hasPermission("dipp.debug")) {
			var regionNames = regions.stream().map(r -> r.getId()).collect(Collectors.joining(", "));
			var error = new TextComponent("No permission to place " + event.getBlock().getType().name() + " in " + regionNames);
			error.setColor(ChatColor.RED);
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, error);
		}
		event.setCancelled(true);
	}

	List<ProtectedRegion> getRegions(org.bukkit.Location location) {
		Location wgLocation = BukkitAdapter.adapt(location);
		World world = BukkitAdapter.adapt(location.getWorld());

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		var regions = query.getApplicableRegions(wgLocation).getRegions();

		RegionManager manager = container.get(world);
		var globalRegion = manager.getRegion("__global__");

		var sortedRegions = regions.stream().sorted((r1, r2) -> r2.getPriority() - r1.getPriority()).collect(Collectors.toList());
		sortedRegions.add(globalRegion);

		return sortedRegions;
	}

}
