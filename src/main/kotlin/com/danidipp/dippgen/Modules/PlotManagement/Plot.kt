package com.danidipp.dippgen.Modules.PlotManagement

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.flags.BooleanFlag
import com.sk89q.worldguard.protection.flags.LocationFlag
import com.sk89q.worldguard.protection.managers.RegionManager
import com.sk89q.worldguard.protection.regions.ProtectedRegion
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import java.util.UUID

data class Plot(
    val region: ProtectedRegion,
    val district: District?,
    val world: World,
) {
    companion object {
        val plotUnlockedFlag = BooleanFlag("plot-unlocked")
        val teleportLocationFlag = LocationFlag("teleport-location")

        fun getPlot(location: Location): Plot? {
            val wgLocation = BukkitAdapter.adapt(location)
            val query = WorldGuard.getInstance().platform.regionContainer.createQuery()
            val plotRegion = query.getApplicableRegions(wgLocation).regions.firstOrNull { it.priority == 5 } ?: return null
            val districtId = plotRegion.id.split("-").firstOrNull()
            return Plot(plotRegion, districtId?.let(District::fromId), location.world)
        }

        fun getPlot(plotId: String): Plot? {
            if (!plotId.contains(":")) return null
            val worldId = plotId.substringBefore(":")
            val regionId = plotId.substringAfter(":")
            val world = Bukkit.getWorld(worldId) ?: return null
            val regionManager: RegionManager = WorldGuard.getInstance().platform.regionContainer.get(BukkitAdapter.adapt(world)) ?: return null
            val region = regionManager.getRegion(regionId) ?: return null
            if (region.priority != 5) return null
            val districtId = region.id.split("-").firstOrNull()
            return Plot(region, districtId?.let(District::fromId), world)
        }

        fun getOwnedPlots(player: Player): Set<Plot> = getOwnedPlots(player.uniqueId)

        fun getOwnedPlots(uuid: UUID?): Set<Plot> {
            val plots = linkedSetOf<Plot>()
            if (uuid == null) return plots

            Bukkit.getWorlds().forEach { world ->
                val regionManager = WorldGuard.getInstance().platform.regionContainer.get(BukkitAdapter.adapt(world)) ?: return@forEach
                regionManager.regions.values.forEach { region ->
                    if (region.owners.playerDomain.contains(uuid)) {
                        val districtId = region.id.split("-").firstOrNull()
                        plots += Plot(region, districtId?.let(District::fromId), world)
                    }
                }
            }
            return plots
        }

        fun getPlotLimit(offlinePlayer: OfflinePlayer?): Int {
            if (offlinePlayer == null) return -1
            val player = offlinePlayer.player ?: return -1
            return player.effectivePermissions
                .asSequence()
                .map { it.permission }
                .filter { it.startsWith("dipp.plot.limit.") }
                .mapNotNull { permission -> permission.substringAfterLast('.').toIntOrNull() }
                .maxOrNull() ?: 0
        }
    }

    val id: String
        get() = "${world.name}:${region.id}"

    val name: String
        get() = region.id.split("-").getOrNull(1) ?: region.id

    fun isManager(player: Player): Boolean {
        val wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player)
        return region.owners.contains(wgPlayer)
    }

    fun isBuilder(player: Player): Boolean {
        val wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player)
        return region.owners.contains(wgPlayer) || region.members.contains(wgPlayer)
    }
}
