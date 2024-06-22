package com.danidipp.dippgen.Events;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class CheckDistrictOnBlockPlace implements Listener {
	enum CheckAction {
		PLACE, BREAK, INTERACT, INTERACT_ENTITY
	}

	public static boolean canDo(CheckAction action, Player player, Location location, Material material) {
		return canDo(action.toString().toLowerCase(), player, location, material.name().toLowerCase());
	}

	public static boolean canDo(CheckAction action, Player player, Location location, EntityType entityType) {
		return canDo(action.toString().toLowerCase(), player, location, entityType.name().toLowerCase());
	}

	private static boolean canDo(String action, Player player, Location location, String thing) {
		if (player.hasPermission("dipp.admin"))
			return true;

		// Check for explicit personal exceptions
		var exceptionName = "dipp.exception." + action + "." + thing;
		var exceptionPerm = player.getEffectivePermissions().stream().filter(p -> p.getPermission().equals(exceptionName)).findFirst();
		if (exceptionPerm.isPresent()) {
			if (exceptionPerm.get().getValue()) {
				// explicit allow
				debugLog(player, null, exceptionName);
				return true;
			} else {
				// explicit deny
				debugLog(player, NamedTextColor.RED, "[" + action.toUpperCase() + "] Explicit exception override: " + exceptionName);
				return false;
			}
		}

		// Check for region permissions
		var regions = getRegions(location);
		if (regions.size() == 0) {
			debugLog(player, NamedTextColor.RED, "[" + action.toUpperCase() + "] No regions found.");
			return false; // Outside the city
		}

		var topRegion = regions.get(0);
		var topRegionIsolatePermission = "dipp." + topRegion.getId() + ".isolate";
		var isPlot = topRegion.getPriority() == 5;
		if (!isPlot && !player.hasPermission(topRegionIsolatePermission)) {
			// Player is not in a plot
			debugLog(player, NamedTextColor.RED, "[" + action.toUpperCase() + "] Region " + topRegion.getId() + " is not isolated.");
			return false;
		}

		var checkedRegions = new ArrayList<String>();
		for (var region : regions) {
			var permission = "dipp." + region.getId() + "." + action + "." + thing;
			if (player.hasPermission(permission)) { // allow event to go through
				debugLog(player, null, permission);
				return true;
			}
			checkedRegions.add(region.getId());

			var isolatedPermission = "dipp." + region.getId() + ".isolate";
			if (player.hasPermission(isolatedPermission) && !isPlot) {
				break;
			}
		}

		debugLog(player, NamedTextColor.RED,
				"[" + action.toUpperCase() + "] No permission to " + action + " " + thing + " in " + String.join(",", checkedRegions));
		return false;
	}

	@EventHandler
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		if (!canDo(
				CheckAction.PLACE.toString().toLowerCase(),
				event.getPlayer(),
				event.getBlock().getLocation(),
				event.getBlockPlaced().getType().name().toLowerCase()))
			event.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		if (!canDo(CheckAction.BREAK, event.getPlayer(), event.getBlock().getLocation(), event.getBlock().getType()))
			event.setCancelled(true);
	}

	// @EventHandler
	// public void onInteract(PlayerInteractEvent event) {
	// 	if (!canDo(CheckAction.INTERACT, event.getPlayer(), event.getClickedBlock().getLocation(), event.getClickedBlock().getType()))
	// 		event.setCancelled(true);
	// }

	// @EventHandler
	// public void onInteractEntity(PlayerInteractEntityEvent event) {
	// 	if (!canDo(CheckAction.INTERACT_ENTITY, event.getPlayer(), event.getRightClicked().getLocation(), event.getRightClicked().getType()))
	// 		event.setCancelled(true);
	// }

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
