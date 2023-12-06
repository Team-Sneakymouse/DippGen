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

public class CheckDistrictOnBlockPlace implements Listener {

	@EventHandler
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		for (var region : getRegions(event.getBlock().getLocation())) {
			var permission = "dipp.placeblock." + region.getId() + "." + event.getBlockPlaced().getType().name().toLowerCase();
			if (player.hasPermission(permission)) { // allow event to go through
				return;
			}

			var isolatedPermission = "dipp.#isolated." + region.getId();
			if (player.hasPermission(isolatedPermission)) {
				break;
			}
		}
		event.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		for (var region : getRegions(event.getBlock().getLocation())) {
			var permission = "dipp.breakblock." + region.getId() + "." + event.getBlock().getType().name().toLowerCase();
			if (player.hasPermission(permission)) { // allow event to go through
				return;
			}

			var isolatedPermission = "dipp.#isolated." + region.getId();
			if (player.hasPermission(isolatedPermission)) {
				break;
			}
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
