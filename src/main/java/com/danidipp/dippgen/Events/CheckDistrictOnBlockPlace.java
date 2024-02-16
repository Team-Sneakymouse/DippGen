package com.danidipp.dippgen.Events;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class CheckDistrictOnBlockPlace implements Listener {
	static boolean canPlace(Player player, Location location, Material material) {
		if (player.hasPermission("dipp.admin"))
			return true;
		var exceptionPermission = "dipp.exception.place." + material.name().toLowerCase();
		if (player.getEffectivePermissions().stream().anyMatch(p -> p.getPermission().equals(exceptionPermission))) {
			if (player.hasPermission(exceptionPermission)) {
				debugLog(player, null, exceptionPermission);
				return true;
			} else {
				debugLog(player, NamedTextColor.RED, "[PLACE] Explicit exception override: " + exceptionPermission);
				return false;
			}
		}

		var regions = getRegions(location);
		if (regions.size() == 0) {
			debugLog(player, NamedTextColor.RED, "[PLACE] No regions found.");
			return false;
		}
		var topRegion = regions.get(0);
		var topRegionIsolatePermission = "dipp." + topRegion.getId() + ".isolate";
		if (topRegion.getPriority() != 5 && !player.hasPermission(topRegionIsolatePermission)) {
			// Player is not in a plot
			debugLog(player, NamedTextColor.RED, "[PLACE] Region " + topRegion.getId() + " is not isolated.");
			return false;
		}

		var checkedRegions = new ArrayList<String>();
		for (var region : regions) {
			var permission = "dipp." + region.getId() + ".place." + material.name().toLowerCase();
			if (player.hasPermission(permission)) { // allow event to go through
				debugLog(player, null, permission);
				return true;
			}
			checkedRegions.add(region.getId());

			var isolatedPermission = "dipp." + region.getId() + ".isolate";
			if (player.hasPermission(isolatedPermission) && topRegion.getPriority() != 5) {
				break;
			}
		}

		debugLog(player, NamedTextColor.RED, "[PLACE] No permission to place " + material.name() + " in " + String.join(",", checkedRegions));
		return false;
	}

	@EventHandler
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		if (!canPlace(event.getPlayer(), event.getBlock().getLocation(), event.getBlockPlaced().getType()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		if (player.hasPermission("dipp.admin"))
			return;
		var exceptionPermission = "dipp.exception.break." + event.getBlock().getType().name().toLowerCase();
		if (player.getEffectivePermissions().stream().anyMatch(p -> p.getPermission().equals(exceptionPermission))) {
			if (player.hasPermission(exceptionPermission)) {
				if (player.hasPermission("dipp.debug")) {
					player.sendActionBar(Component.text(exceptionPermission));
				}
				return;
			} else {
				debugLog(player, NamedTextColor.RED, "[BREAK] Explicit exception override: " + exceptionPermission);
				event.setCancelled(true);
				return;
			}
		}

		var regions = getRegions(event.getBlock().getLocation());
		if (regions.size() == 0) {
			debugLog(player, NamedTextColor.RED, "[BREAK] No regions found.");
			event.setCancelled(true);
			return;
		}
		var topRegion = regions.get(0);
		var topRegionIsolatePermission = "dipp." + topRegion.getId() + ".isolate";
		if (topRegion.getPriority() != 5 && !player.hasPermission(topRegionIsolatePermission)) {
			// Player is not in a plot
			debugLog(player, NamedTextColor.RED, "[BREAK] Region " + topRegion.getId() + " is not isolated.");
			event.setCancelled(true);
			return;
		}

		var checkedRegions = new ArrayList<String>();
		for (var region : regions) {
			var permission = "dipp." + region.getId() + ".break." + event.getBlock().getType().name().toLowerCase();
			if (player.hasPermission(permission)) { // allow event to go through
				debugLog(player, null, permission);
				return;
			}
			checkedRegions.add(region.getId());

			var isolatedPermission = "dipp." + region.getId() + ".isolate";
			if (player.hasPermission(isolatedPermission) && topRegion.getPriority() != 5) {
				break;
			}
		}
		debugLog(player, NamedTextColor.RED,
				"[BREAK] No permission to break " + event.getBlock().getType().name() + " in " + String.join(",", checkedRegions));
		event.setCancelled(true);
	}

	static List<ProtectedRegion> getRegions(Location location) {
		var wgLocation = BukkitAdapter.adapt(location);

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		var regions = query.getApplicableRegions(wgLocation).getRegions();

		var sortedRegions = regions.stream().sorted((r1, r2) -> r2.getPriority() - r1.getPriority()).collect(Collectors.toList());
		return sortedRegions;
	}

	static void debugLog(Player player, @Nullable TextColor color, String message) {
		if (player.hasPermission("dipp.debug")) {
			var error = Component.text(message);
			if (color != null)
				error.color(color);
			player.sendActionBar(error);
			;
		}
	}

}
