package com.danidipp.dippgen.Events;

import java.util.TimerTask;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;

import com.danidipp.dippgen.Plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.api.chat.hover.content.Text;

public class SpyOnBookSign implements Listener {
	@EventHandler
	void onPlayerEditBookEvent(PlayerEditBookEvent event) {
		if (!event.isSigning())
			return;

		var bookMeta = event.getNewBookMeta();
		var bookId = bookMeta.hashCode();
		Plugin.plugin.recentBooks.put("" + bookId, bookMeta);
		Bukkit.getScheduler().runTaskLater(Plugin.plugin, new TimerTask() {
			public void run() {
				Plugin.plugin.recentBooks.remove("" + bookId);
			}
		}, 12000); // 10 mins

		var playerText = new TextComponent(event.getPlayer().getDisplayName());
		playerText.setColor(ChatColor.YELLOW);
		playerText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Teleport to player")));
		playerText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minecraft:tp " + event.getPlayer().getName()));

		var bookText = new TextComponent(bookMeta.getTitle());
		bookText.setColor(ChatColor.GOLD);
		bookText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
				new Item(Material.WRITTEN_BOOK.getKey().toString(), 1, ItemTag.ofNbt(bookMeta.getAsString()))));
		bookText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Plugin.plugin.getName() + ":showbook " + bookId));

		var text = new TextComponent();
		text.addExtra(Plugin.LOG_PREFIX);
		text.addExtra(playerText);
		text.addExtra(" signed book ");
		text.addExtra(bookText);

		for (var player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.hasPermission("dipp.bookspy")) {
				player.spigot().sendMessage(text);
			}
		}

	}

}
