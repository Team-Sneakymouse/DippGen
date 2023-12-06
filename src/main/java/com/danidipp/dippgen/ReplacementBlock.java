package com.danidipp.dippgen;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;

public record ReplacementBlock(Material material, int weight) {
	public Map<String, ?> toMap() {
		var map = new HashMap<String, Object>();
		map.put("material", material.name());
		map.put("weight", weight);
		return map;
	}
};