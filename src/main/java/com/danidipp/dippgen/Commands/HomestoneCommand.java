package com.danidipp.dippgen.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.danidipp.dippgen.Plugin;
import com.danidipp.dippgen.Modules.PlotManagement.Plot;
import com.danidipp.dippgen.Modules.PlotManagement.PlotDeed;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HomestoneCommand implements ICommandImpl, Listener {
	@Override
	public String getName() {
		return "homestone";
	}

	@Override
	public CommandExecutor getExecutor() {
		return (sender, command, label, args) -> {
			if (args.length < 1)
				return false;
			Player player = Bukkit.getPlayer(args[0]);
			if (player == null)
				return false;

			var plot = Plot.getPlot(player.getLocation());
			if (plot != null && Plot.getOwnedPlots(player).contains(plot)) {
				plot.region().setFlag(Plot.teleportLocationFlag, BukkitAdapter.adapt(player.getLocation()));
				player.sendActionBar(Component.text("The teleport location for this plot has been updated").color(NamedTextColor.GREEN));
				return true;
			} else {
				var inventory = createInventory(player);
				player.openInventory(inventory);
				return true;
			}
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		// TODO Auto-generated method stub
		return null;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getHolder() != inventoryHolder) return;
		event.setCancelled(true);
		var player = (Player) event.getWhoClicked();

		if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != inventoryHolder || event.getCurrentItem() == null)
			return;
		var item = event.getCurrentItem();
		var meta = item.getItemMeta();
		var plotId = meta.getPersistentDataContainer().get(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING);
		var plot = Plot.getPlot(plotId);
		if (plot == null) {
			player.sendMessage(Component.text("Error: Couldn't get plot with id \"" + plotId + "\". Please tell Dani!").color(NamedTextColor.RED));
			return;
		}
		var tpLocation = plot.region().getFlag(Plot.teleportLocationFlag);
		if (tpLocation == null) {
			player.sendMessage(Component.text("This plot doesn't have a teleport destination set up").color(NamedTextColor.RED));
			Bukkit.getScheduler().runTask(Plugin.plugin, () -> event.getView().close());
			return;
		}
		String command = "ms cast as " + player.getName() + " homestone " + tpLocation.getBlockX() + " " + tpLocation.getBlockY() + " "
				+ tpLocation.getBlockZ() + " " + tpLocation.getYaw() + " " + tpLocation.getPitch();
		// command = command.replace("ms cast as " + player.getName() + " homestone ",
		// 		"minecraft:tp " + player.getName() + " ");
		player.sendMessage(command);
		Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
		Bukkit.getScheduler().runTask(Plugin.plugin, () -> event.getView().close());
	}

	static Inventory createInventory(Player player) {
		var plots = Plot.getOwnedPlots(player);
		var inventorySlots = (plots.size() / 9 + 1) * 9;
		var inventory = Bukkit.createInventory(inventoryHolder, inventorySlots,
				Component.text("Choose a plot to teleport to").color(NamedTextColor.GOLD));
		var plotsList = plots.stream().toList();
		for (int i = 0; i < plotsList.size(); i++) {
			var plot = plotsList.get(i);
			var item = new ItemStack(plot.district().material());
			var meta = item.getItemMeta();
			meta.displayName(Component.text(plot.getName()).color(NamedTextColor.YELLOW));
			// meta.lore(List.of(Component.text("").color(NamedTextColor.GRAY)));
			meta.getPersistentDataContainer().set(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING, plot.getId());
			item.setItemMeta(meta);
			inventory.setItem(i, item);
		}
		return inventory;
	}

	public static InventoryHolder inventoryHolder = new InventoryHolder() {
		@Override
		public Inventory getInventory() {
			return null;
		}
	};
}
