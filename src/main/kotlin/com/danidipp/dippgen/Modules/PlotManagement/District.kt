package com.danidipp.dippgen.Modules.PlotManagement

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Location
import org.bukkit.Material

data class District(
    val name: String,
    val id: String,
    val material: Material,
    val deed: PlotDeed,
) {
    companion object {
        val districts: Set<District> = linkedSetOf(
            District("The Slums", "slums", Material.PODZOL, PlotDeed("Slums Deed", "Cost: 5 Silver", 24)),
            District("Moon Bay", "moonbay", Material.LIGHT_BLUE_CONCRETE_POWDER, PlotDeed("Moon Bay Deed", "Cost: 100 Gold", 28)),
            District("Darkvale", "darkvale", Material.NETHER_BRICKS, PlotDeed("Darkvale Deed", "Cost: 666 Gold", 31)),
            District("Sticky District", "sticky", Material.HONEYCOMB_BLOCK, PlotDeed("Sticky District Deed", "Cost: 20 Gold", 33)),
            District("Brickton", "brickton", Material.BRICKS, PlotDeed("Brickton Deed", "Cost: 50 Gold", 30)),
            District("The Grove", "grove", Material.OAK_LEAVES, PlotDeed("Grove Deed", "Cost: 75 Gold", 25)),
            District("Southshire", "southshire", Material.LAPIS_BLOCK, PlotDeed("South Shire Deed", "Cost: 100 Gold", 23)),
            District("Goat Town", "goattown", Material.CHERRY_LEAVES, PlotDeed("Goat Town Deed", "Cost: 55 Gold", 29)),
            District("Dwarven District", "dwarven", Material.COBBLESTONE, PlotDeed("Dwarven Deed", "Cost: 50 Gold", 26)),
            District("Brightvale", "brightvale", Material.QUARTZ_BLOCK, PlotDeed("Brightvale Deed", "Cost: 10,000 Gold", 32)),
            District("Royal District", "royal", Material.CRAFTING_TABLE, PlotDeed("Royal Deed", "Unobtainable", -1)),
        )

        fun fromId(id: String): District? = districts.firstOrNull { it.id == id }

        fun fromLocation(location: Location): District? {
            val wgLocation = BukkitAdapter.adapt(location)
            val query = WorldGuard.getInstance().platform.regionContainer.createQuery()
            val regions: Set<ProtectedRegion> = query.getApplicableRegions(wgLocation).regions
            return regions.asSequence().mapNotNull { fromId(it.id) }.firstOrNull()
        }
    }
}
