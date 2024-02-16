package com.danidipp.dippgen.Events;

import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class WritingSkill implements Listener {
	@EventHandler
	public void onSignEdit(SignChangeEvent event) {
		var player = event.getPlayer();
		var maxLines = player.getEffectivePermissions().stream().filter(p -> p.getPermission().startsWith("dipp.signs.lines.")).map(p -> {
			var number = p.getPermission().substring("dipp.signs.lines.".length());
			return Integer.parseInt(number);
		}).max(Integer::compare).orElse(0);

		if (player.hasPermission("dipp.debug")) {
			((Audience) player).sendActionBar(Component.text("Max lines: " + maxLines).color(NamedTextColor.GREEN));
		}

		var linesRemoved = 0;
		for (var i = 0; i < event.lines().size(); i++) {
			var line = (TextComponent) event.line(i);
			if (line.content().isBlank()) { // Empty line, ignore
				var newLine = line.content("");
				event.line(i, newLine);
				continue;
			} else if (maxLines > 0) { // Not empty, but not at limit
				maxLines--;
			} else { // Not empty, past limit
				var newLine = line.font(Key.key("lom", "dwarven"));
				event.line(i, newLine);
				linesRemoved++;
				// Plugin.plugin.getLogger().info("New line: " + JSONComponentSerializer.json().serialize(newLine));
			}
		}

		if (linesRemoved > 0) {
			((Audience) player).sendActionBar(Component.text("You are not skilled enough to write all that").color(NamedTextColor.RED));
		}
	}

	// Prevent initial interaction with sign if player doesn't have building permission in that spot
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null || !Tag.ALL_SIGNS.isTagged(event.getClickedBlock().getType())) {
			return;
		}

		var player = event.getPlayer();
		var wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		boolean canBypass = WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(wgPlayer, wgPlayer.getWorld());
		if (canBypass) {
			return;
		}

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		var wgLocation = BukkitAdapter.adapt(event.getClickedBlock().getLocation());

		if (!query.testState(wgLocation, wgPlayer, Flags.BUILD)
				|| !CheckDistrictOnBlockPlace.canPlace(player, event.getClickedBlock().getLocation(), event.getClickedBlock().getType())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBookEdit(PlayerEditBookEvent event) {
		var bookMeta = event.getNewBookMeta();

	}

}
