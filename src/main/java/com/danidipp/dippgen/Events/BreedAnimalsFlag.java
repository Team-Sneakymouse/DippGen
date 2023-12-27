package com.danidipp.dippgen.Events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

import com.danidipp.dippgen.Plugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;

public class BreedAnimalsFlag implements Listener {
	public static StateFlag breedAnimalsFlag = new StateFlag("breed-animals", true);

	@EventHandler
	public void onAnimalBreeding(EntityBreedEvent event) {
		if (event.isCancelled())
			return;

		var wgPlayer = WorldGuardPlugin.inst().wrapPlayer((Player) event.getBreeder());
		var wgLocation = BukkitAdapter.adapt(event.getEntity().getLocation());
		var container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		var query = container.createQuery();
		var regions = query.getApplicableRegions(wgLocation);
		if (regions.testState(wgPlayer, breedAnimalsFlag))
			return;

		event.setCancelled(true);
		Bukkit.getServer().broadcast("Animal breeding cancelled", "dippgen.debug");

		var mother = event.getMother();
		var father = event.getFather();
		if (!(mother instanceof Breedable) || !(father instanceof Breedable)) {
			Plugin.plugin.getLogger().warning(
					"EntityBreedEvent triggered on non-breedable entity: " + mother.getType().toString() + " and " + father.getType().toString());
			return;
		}
		((Breedable) mother).setBreed(false);
		((Breedable) father).setBreed(false);

	}

}
