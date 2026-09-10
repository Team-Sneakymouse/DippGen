package com.danidipp.dippgen

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

object ReplacementConfigParser {
    fun parse(plugin: Plugin): MutableList<Replacement> {
        plugin.saveDefaultConfig()

        if (!plugin.config.getKeys(false).contains("replacements")) {
            val defaults = plugin.config.getConfigurationSection("replacements")?.defaultSection
            plugin.config.set("replacements", defaults)
        }

        val replacementsSection = plugin.config.getConfigurationSection("replacements") ?: run {
            plugin.logger.warning("No replacements section found in config")
            plugin.saveConfig()
            return mutableListOf()
        }

        val replacements = mutableListOf<Replacement>()
        replacementsSection.getKeys(false).forEach { replacementName ->
            val configReplacement = replacementsSection.getConfigurationSection(replacementName)
            if (configReplacement == null) {
                plugin.logger.warning("Replacement definition at replacements.$replacementName is not a section")
                return@forEach
            }

            val blocks = parseBlocks(plugin, replacementName, configReplacement)
            if (blocks.isEmpty()) {
                plugin.logger.warning("Replacement definition at replacements.$replacementName invalid: blocks is missing")
                return@forEach
            }

            val locations = parseLocations(plugin, replacementName, configReplacement)
            val regions = parseRegions(plugin, replacementName, configReplacement)
            val minDelay = configReplacement.getLong("minDelay", 120L) * 20L
            val maxDelay = configReplacement.getLong("maxDelay", 120L) * 20L

            replacements += Replacement(replacementName, blocks, minDelay, maxDelay, locations, regions)
        }

        plugin.logger.info("Loaded replacements: $replacements")
        plugin.saveConfig()
        return replacements
    }

    private fun parseBlocks(
        plugin: Plugin,
        replacementName: String,
        section: ConfigurationSection,
    ): Set<ReplacementBlock> {
        val configBlocks = section.getMapList("blocks")
        if (configBlocks.isEmpty()) {
            return emptySet()
        }

        val blocks = linkedSetOf<ReplacementBlock>()
        configBlocks.forEach { map ->
            val materialName = map["material"] as? String
            val material = materialName?.let(Material::getMaterial)
            if (material == null) {
                plugin.logger.warning("Unknown material for replacement $replacementName: $materialName")
                return@forEach
            }

            val weight = (map["weight"] as? Number)?.toInt() ?: 1
            blocks += ReplacementBlock(material, weight)
        }
        return blocks
    }

    private fun parseLocations(
        plugin: Plugin,
        replacementName: String,
        section: ConfigurationSection,
    ): MutableList<Location> {
        val world = Bukkit.getWorlds().firstOrNull()
        if (world == null) {
            plugin.logger.warning("No worlds loaded while parsing replacements")
            return mutableListOf()
        }

        val locations = mutableListOf<Location>()
        section.getStringList("coordinates").forEach { configLocation ->
            val split = configLocation.split(" ")
            if (split.size != 3) {
                plugin.logger.warning("Failed to parse location for replacement $replacementName: $configLocation")
                return@forEach
            }

            val x = split[0].toIntOrNull()
            val y = split[1].toIntOrNull()
            val z = split[2].toIntOrNull()
            if (x == null || y == null || z == null) {
                plugin.logger.warning("Failed to parse location for replacement $replacementName: $configLocation")
                return@forEach
            }

            locations += Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        }

        return locations
    }

    private fun parseRegions(
        plugin: Plugin,
        replacementName: String,
        section: ConfigurationSection,
    ): MutableList<ProtectedRegion> {
        val bukkitWorld = Bukkit.getWorld("world")
        if (bukkitWorld == null) {
            return mutableListOf()
        }

        val wgWorld = BukkitAdapter.adapt(bukkitWorld)
        val regionManager = WorldGuard.getInstance().platform.regionContainer.get(wgWorld)
        if (regionManager == null) {
            plugin.logger.warning("No WorldGuard region manager available for world")
            return mutableListOf()
        }

        val configRegions = section.getStringList("regions")
        val regions = mutableListOf<ProtectedRegion>()
        configRegions.forEach { configRegion ->
            val region = regionManager.getRegion(configRegion)
            if (region == null) {
                plugin.logger.warning("Unknown region for replacement $replacementName: $configRegion")
            } else {
                regions += region
            }
        }
        return regions
    }
}
