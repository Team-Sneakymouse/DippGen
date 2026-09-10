package com.danidipp.dippgen

import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.Waterlogged
import org.bukkit.block.data.type.PinkPetals
import java.util.concurrent.ThreadLocalRandom

data class ReplacementBlock(val material: Material, val weight: Int) {
    fun toMap(): Map<String, Any> = mapOf(
        "material" to material.name,
        "weight" to weight,
    )
}

data class Replacement(
    val name: String,
    val blocks: Set<ReplacementBlock>,
    val minDelay: Long,
    val maxDelay: Long,
    val locations: MutableList<Location>,
    val regions: MutableList<ProtectedRegion>,
) {
    private fun randomMaterial(): Material {
        require(blocks.isNotEmpty()) { "Can't select a block from an empty list" }
        val totalWeight = blocks.sumOf { it.weight }.toDouble()
        var randomWeight = ThreadLocalRandom.current().nextDouble(totalWeight)

        blocks.forEach { block ->
            randomWeight -= block.weight
            if (randomWeight < 0.0) {
                return block.material
            }
        }
        return blocks.first().material
    }

    fun placeBlock(block: Block) {
        val oldMaterial = block.type
        val newMaterial = randomMaterial()
        block.setType(newMaterial, false)

        val data = block.blockData
        if (data is Waterlogged) {
            data.isWaterlogged = oldMaterial == Material.WATER
            block.setBlockData(data, false)
        }

        if (data is Bisected) {
            data.half = Bisected.Half.BOTTOM
            block.setBlockData(data, false)
            val topBlock = block.getRelative(BlockFace.UP)
            if (topBlock.type.isAir) {
                topBlock.setType(newMaterial, false)
                val topData = topBlock.blockData
                if (topData is Bisected) {
                    topData.half = Bisected.Half.TOP
                    topBlock.setBlockData(topData, false)
                }
            }
        }

        if (newMaterial == Material.PINK_PETALS && data is PinkPetals) {
            data.flowerAmount = ThreadLocalRandom.current().nextInt(1, 5)
            block.setBlockData(data, false)
        }
    }

    fun toMap(): Map<String, Any> = mapOf(
        "blocks" to blocks.map { it.toMap() },
        "minDelay" to minDelay / 20L,
        "maxDelay" to maxDelay / 20L,
        "coordinates" to locations.map { "${it.blockX} ${it.blockY} ${it.blockZ}" },
        "regions" to regions.map { it.id },
    )
}
