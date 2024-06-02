package com.danidipp.dippgen.Events;

import java.util.TimerTask;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;

import com.danidipp.dippgen.Plugin;

import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class SpyOnBookSign implements Listener {
	@EventHandler
	void onPlayerEditBookEvent(PlayerEditBookEvent event) {
		if (!event.isSigning()) return;

		var bookMeta = event.getNewBookMeta();
		var bookId = bookMeta.hashCode();
		Plugin.plugin.recentBooks.put("" + bookId, bookMeta);
		Bukkit.getScheduler().runTaskLater(Plugin.plugin, new TimerTask() {
			public void run() {
				Plugin.plugin.recentBooks.remove("" + bookId);
			}
		}, 12000); // 10 mins

		var playerText = event.getPlayer().displayName().color(NamedTextColor.YELLOW)
				.hoverEvent(HoverEvent.showText(Component.text("Teleport to player")))
				.clickEvent(ClickEvent.runCommand("/minecraft:tp " + event.getPlayer().getName()));

		var bookNbt = BinaryTagHolder.binaryTagHolder(bookMeta.getAsString());
		var bookText = bookMeta.title().color(NamedTextColor.GOLD)
				.hoverEvent(HoverEvent.showItem(Material.WRITTEN_BOOK.getKey().key(), 1, bookNbt))
				.clickEvent(ClickEvent.runCommand("/" + Plugin.plugin.getName() + ":showbook " + bookId));

		var message = Plugin.LOG_PREFIX
				.append(playerText)
				.append(Component.text(" signed book "))
				.append(bookText);

		for (var player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.hasPermission("dipp.bookspy")) {
				player.sendMessage(message);
			}
		}

	}

}
