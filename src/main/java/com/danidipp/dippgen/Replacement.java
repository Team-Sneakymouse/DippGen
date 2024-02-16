package com.danidipp.dippgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;

public record Replacement(String name, Set<ReplacementBlock> blocks, long minDelay, long maxDelay, List<Location> locations) {

	public Material getRandomMaterial() {
		if (this.blocks.size() == 0)
			throw new IllegalStateException("Can't select a block from an empty list");

		var totalWeight = this.blocks().stream().mapToDouble(ReplacementBlock::weight).sum();
		var randomWeight = new Random().nextDouble(totalWeight);

		for (var replacementBlock : this.blocks()) {
			randomWeight -= replacementBlock.weight();
			if (randomWeight < 0) {
				return replacementBlock.material();
			}
		}

		throw new IllegalStateException("Unable to select a material");
	}

	public Map<String, ?> toMap() {
		var map = new HashMap<String, Object>();
		map.put("blocks", blocks.stream().map(ReplacementBlock::toMap).collect(Collectors.toList()));
		map.put("minDelay", minDelay / 20);
		map.put("maxDelay", maxDelay / 20);
		map.put("coordinates", locations.stream().map(l -> l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()).collect(Collectors.toList()));

		return map;
	}
};
