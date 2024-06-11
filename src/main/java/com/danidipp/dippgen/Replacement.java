package com.danidipp.dippgen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.type.PinkPetals;
import org.checkerframework.checker.units.qual.t;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public record Replacement(String name, Set<ReplacementBlock> blocks, long minDelay, long maxDelay, List<Location> locations,
		List<ProtectedRegion> regions) {

	private Material getRandomMaterial() {
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

	public void placeBlock(Block block) {
		var oldMaterial = block.getType();
		var newMaterial = getRandomMaterial();
		block.setType(newMaterial, false);
		var data = block.getBlockData();
		if (data instanceof Waterlogged) {
			((Waterlogged) data).setWaterlogged(oldMaterial == Material.WATER);
			block.setBlockData(data, false);
		}

		if (data instanceof Bisected) {
			((Bisected) data).setHalf(Half.BOTTOM);
			var topBlock = block.getRelative(BlockFace.UP);
			if (topBlock.getType().isAir()) {
				topBlock.setType(newMaterial, false);
				var topData = topBlock.getBlockData();
				((Bisected) topData).setHalf(Half.TOP);
				topBlock.setBlockData(topData, false);
			}
		}

		if (newMaterial == Material.PINK_PETALS) {
			var count = new Random().nextInt(4) + 1;
			((PinkPetals) data).setFlowerAmount(count);
			block.setBlockData(data, false);
		}

	}

	public Map<String, ?> toMap() {
		var map = new HashMap<String, Object>();
		map.put("blocks", blocks.stream().map(ReplacementBlock::toMap).collect(Collectors.toList()));
		map.put("minDelay", minDelay / 20);
		map.put("maxDelay", maxDelay / 20);
		map.put("coordinates", locations.stream().map(l -> l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ()).collect(Collectors.toList()));
		map.put("regions", regions.stream().map(ProtectedRegion::getId).collect(Collectors.toList()));

		return map;
	}
};
