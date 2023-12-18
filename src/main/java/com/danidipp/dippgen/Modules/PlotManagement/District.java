package com.danidipp.dippgen.Modules.PlotManagement;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

public record District(String name, String id, PlotDeed deed) {

	public static Set<District> districts = new HashSet<District>() {
		{
			add(new District("The Slums", "slums", new PlotDeed("§6Slums Deed", "§7Cost: 5 Silver", 24)));
			add(new District("Moon Bay", "moonbay", new PlotDeed("§6Moon Bay Deed", "§7Cost: 100 Gold", 28)));
			add(new District("Darkvale", "darkvale", new PlotDeed("§6Darkvale Deed", "§7Cost: 666 Gold", 31)));
			add(new District("Stick District", "sticky", new PlotDeed("§6Stick District Deed", "§7Cost: 20 Gold", 33)));
			add(new District("Brickton", "brickton", new PlotDeed("§6Brickton Deed", "§7Cost: 50 Gold", 30)));
			add(new District("The Grove", "grove", new PlotDeed("§6Grove Deed", "§7Cost: 75 Gold", 25)));
			add(new District("Southshire", "southshire", new PlotDeed("§6South Shire Deed", "§7Cost: 100 Gold", 23)));
			add(new District("Goat Town", "goattown", new PlotDeed("§6Goat Town Deed", "§7Cost: 55 Gold", 29)));
			add(new District("Dwarven District", "dwarven", new PlotDeed("§6Dwarven Deed", "§7Cost: 50 Gold", 26)));
			add(new District("Brightvale", "brightvale", new PlotDeed("§6Brightvale Deed", "§7Cost: 10,000 Gold", 32)));
		}
	};

	@Nullable
	public static District fromId(String id) {
		return districts.stream().filter(d -> d.id().equals(id)).findFirst().orElse(null);
	}
}