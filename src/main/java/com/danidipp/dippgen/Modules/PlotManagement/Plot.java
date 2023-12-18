package com.danidipp.dippgen.Modules.PlotManagement;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.IntegerFlag;
import com.sk89q.worldguard.protection.flags.LocationFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public record Plot(ProtectedRegion region, @Nullable District district, World world) {

	public static IntegerFlag maxMembersFlag = new IntegerFlag("max-members");
	public static BooleanFlag plotUnlockedFlag = new BooleanFlag("plot-unlocked");
	public static LocationFlag teleportLocationFlag = new LocationFlag("teleport-location");

	@Nullable
	public static Plot getPlot(Location location) {
		var wgLocation = BukkitAdapter.adapt(location);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		var regions = query.getApplicableRegions(wgLocation).getRegions();
		var plotRegion = regions.stream().filter(r -> r.getPriority() == 5).findFirst().orElse(null);
		if (plotRegion == null)
			return null;

		try {
			var districtId = plotRegion.getId().split("-")[0];
			var district = District.fromId(districtId);
			return new Plot(plotRegion, district, location.getWorld());
		} catch (NullPointerException e) {
			return new Plot(plotRegion, null, location.getWorld());
		}
	}

	@Nullable
	public static Plot getPlot(String plotId) {
		if (!plotId.contains(":"))
			return null;
		String worldId = plotId.split(":")[0];
		String regionId = plotId.split(":")[1];

		var world = Bukkit.getWorld(worldId);
		if (world == null)
			return null;

		var wgWorld = BukkitAdapter.adapt(world);
		var regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
		var regionManager = regionContainer.get(wgWorld);
		var region = regionManager.getRegion(regionId);
		if (region == null || region.getPriority() != 5)
			return null;

		try {
			var districtId = region.getId().split("-")[0];
			var district = District.fromId(districtId);
			return new Plot(region, district, world);
		} catch (NullPointerException e) {
			return new Plot(region, null, world);
		}
	}

	public static Set<Plot> getPlots(Player player) {
		return getPlots(player.getUniqueId());
	}

	public static Set<Plot> getPlots(@Nullable UUID uuid) {
		Set<Plot> plots = new HashSet<Plot>();
		if (uuid == null)
			return plots;

		var worlds = Bukkit.getWorlds();
		for (var world : worlds) {
			// Get all regions
			var wgWorld = BukkitAdapter.adapt(world);
			RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
			Collection<ProtectedRegion> regions = regionContainer.get(wgWorld).getRegions().values();

			for (var region : regions) {
				if (!region.getOwners().contains(uuid))
					continue;
				try {
					var districtId = region.getId().split("-")[0];
					var district = District.fromId(districtId);
					plots.add(new Plot(region, district, world));
				} catch (NullPointerException e) {
					plots.add(new Plot(region, null, world));
				}
			}
		}

		return plots;
	}

	static int getPlotLimit(@Nullable OfflinePlayer offlinePlayer) {
		if (offlinePlayer == null)
			return -1;
		var player = offlinePlayer.getPlayer();
		if (player == null)
			return -1;

		return player.getEffectivePermissions().stream().filter(p -> p.getPermission().startsWith("dipp.plot.limit.")).map(p -> {
			var split = p.getPermission().split("\\.");
			return Integer.parseInt(split[split.length - 1]);
		}).max(Integer::compare).orElse(0);

	}

	public String getId() {
		return this.world.getName() + ":" + this.region.getId();
	}

	public int getMemberLimit() {
		var maxMembersFlag = this.region.getFlag(Plot.maxMembersFlag);
		return maxMembersFlag != null ? maxMembersFlag.intValue() : 1;
	}
}
