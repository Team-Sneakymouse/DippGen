package com.danidipp.dippgen.Events

import com.danidipp.dippgen.Plugin
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.bukkit.WorldGuardPlugin
import com.sk89q.worldguard.protection.flags.StateFlag
import org.bukkit.entity.Breedable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent

class BreedAnimalsFlag : Listener {
    companion object {
        val breedAnimalsFlag = StateFlag("breed-animals", true)
    }

    @EventHandler
    fun onAnimalBreeding(event: EntityBreedEvent) {
        if (event.isCancelled) {
            return
        }

        val breeder = event.breeder as? Player ?: return
        val wgPlayer = WorldGuardPlugin.inst().wrapPlayer(breeder)
        val wgLocation = BukkitAdapter.adapt(event.entity.location)
        val query = WorldGuard.getInstance().platform.regionContainer.createQuery()
        val regions = query.getApplicableRegions(wgLocation)

        if (!regions.testState(wgPlayer, breedAnimalsFlag)) {
            event.isCancelled = true
            val mother = event.mother
            val father = event.father
            if (mother is Breedable && father is Breedable) {
                mother.setBreed(false)
                father.setBreed(false)
            } else {
                Plugin.plugin.logger.warning(
                    "EntityBreedEvent triggered on non-breedable entity: ${mother.type} and ${father.type}",
                )
            }
        }
    }
}
