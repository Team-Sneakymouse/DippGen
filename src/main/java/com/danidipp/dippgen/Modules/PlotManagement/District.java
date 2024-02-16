package com.danidipp.dippgen.Modules.PlotManagement;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public record District(String name, String id, Material material, PlotDeed deed) {

	public static Set<District> districts = new HashSet<District>() {
		{
			add(new District("The Slums", "slums", Material.PODZOL, new PlotDeed("Slums Deed", "Cost: 5 Silver", 24)));
			add(new District("Moon Bay", "moonbay", Material.LIGHT_BLUE_CONCRETE_POWDER, new PlotDeed("Moon Bay Deed", "Cost: 100 Gold", 28)));
			add(new District("Darkvale", "darkvale", Material.NETHER_BRICKS, new PlotDeed("Darkvale Deed", "Cost: 666 Gold", 31)));
			add(new District("Sticky District", "sticky", Material.HONEYCOMB_BLOCK, new PlotDeed("Sticky District Deed", "Cost: 20 Gold", 33)));
			add(new District("Brickton", "brickton", Material.BRICKS, new PlotDeed("Brickton Deed", "Cost: 50 Gold", 30)));
			add(new District("The Grove", "grove", Material.OAK_LEAVES, new PlotDeed("Grove Deed", "Cost: 75 Gold", 25)));
			add(new District("Southshire", "southshire", Material.LAPIS_BLOCK, new PlotDeed("South Shire Deed", "Cost: 100 Gold", 23)));
			add(new District("Goat Town", "goattown", Material.CHERRY_LEAVES, new PlotDeed("Goat Town Deed", "Cost: 55 Gold", 29)));
			add(new District("Dwarven District", "dwarven", Material.COBBLESTONE, new PlotDeed("Dwarven Deed", "Cost: 50 Gold", 26)));
			add(new District("Brightvale", "brightvale", Material.QUARTZ_BLOCK, new PlotDeed("Brightvale Deed", "Cost: 10,000 Gold", 32)));
			add(new District("Royal District", "royal", Material.CRAFTING_TABLE, new PlotDeed("Royal Deed", "Unobtainable", -1)));
		}
	};

	@Nullable
	public static District fromId(String id) {
		return districts.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null);
	}

	@Nullable
	public static District fromLocation(Location location) {
		var wgLocation = BukkitAdapter.adapt(location);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		var regions = query.getApplicableRegions(wgLocation).getRegions();

		var district = regions.stream().map(r -> District.fromId(r.getId())).filter(d -> d != null).findFirst().orElse(null);
		return district;
	}
}
