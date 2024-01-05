package com.danidipp.dippgen.Events;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.WorldGuard;
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
		var exceptionPermission = "dipp.exception.place." + event.getBlock().getType().name().toLowerCase();
		if (player.getEffectivePermissions().stream().anyMatch(p -> p.getPermission().equals(exceptionPermission))) {
			if (player.hasPermission(exceptionPermission)) {
				debugLog(player, null, exceptionPermission);
				return;
			} else {
				debugLog(player, ChatColor.RED, "[PLACE] Explicit exception override: " + exceptionPermission);
				event.setCancelled(true);
				return;
			}
		}

		var regions = getRegions(event.getBlock().getLocation());
		if (regions.size() == 0) {
			debugLog(player, ChatColor.RED, "[PLACE] No regions found.");
			event.setCancelled(true);
			return;
		}
		var topRegionIsolatePermission = "dipp." + regions.get(0).getId() + ".isolate";
		if (regions.get(0).getPriority() != 5 && !player.hasPermission(topRegionIsolatePermission)) {
			// Player is not in a plot
			debugLog(player, ChatColor.RED, "[PLACE] Region " + regions.get(0).getId() + " is not isolated.");
			event.setCancelled(true);
			return;
		}

		var checkedRegions = new ArrayList<String>();
		for (var region : regions) {
			var permission = "dipp." + region.getId() + ".place." + event.getBlockPlaced().getType().name().toLowerCase();
			if (player.hasPermission(permission)) { // allow event to go through
				debugLog(player, null, permission);
				return;
			}
			checkedRegions.add(region.getId());

			var isolatedPermission = "dipp." + region.getId() + ".isolate";
			if (player.hasPermission(isolatedPermission)) {
				break;
			}
		}

		debugLog(player, ChatColor.RED,
				"[PLACE] No permission to place " + event.getBlockPlaced().getType().name() + " in " + String.join(",", checkedRegions));
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
					player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(exceptionPermission));
				}
				return;
			} else {
				debugLog(player, ChatColor.RED, "[BREAK] Explicit exception override: " + exceptionPermission);
				event.setCancelled(true);
				return;
			}
		}

		var regions = getRegions(event.getBlock().getLocation());
		if (regions.size() == 0) {
			debugLog(player, ChatColor.RED, "[BREAK] No regions found.");
			event.setCancelled(true);
			return;
		}
		var topRegionIsolatePermission = "dipp." + regions.get(0).getId() + ".isolate";
		if (regions.get(0).getPriority() != 5 && !player.hasPermission(topRegionIsolatePermission)) {
			// Player is not in a plot
			debugLog(player, ChatColor.RED, "[BREAK] Region " + regions.get(0).getId() + " is not isolated.");
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
			if (player.hasPermission(isolatedPermission)) {
				break;
			}
		}
		debugLog(player, ChatColor.RED,
				"[BREAK] No permission to break " + event.getBlock().getType().name() + " in " + String.join(",", checkedRegions));
		event.setCancelled(true);
	}

	List<ProtectedRegion> getRegions(org.bukkit.Location location) {
		Location wgLocation = BukkitAdapter.adapt(location);

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		var regions = query.getApplicableRegions(wgLocation).getRegions();

		var sortedRegions = regions.stream().sorted((r1, r2) -> r2.getPriority() - r1.getPriority()).collect(Collectors.toList());
		return sortedRegions;
	}

	void debugLog(Player player, @Nullable ChatColor color, String message) {
		if (player.hasPermission("dipp.debug")) {
			var error = new TextComponent(message);
			if (color != null)
				error.setColor(color);
			player.spigot().sendMessage(ChatMessageType.ACTION_BAR, error);
		}
	}

}
