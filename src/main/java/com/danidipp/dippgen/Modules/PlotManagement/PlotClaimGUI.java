package com.danidipp.dippgen.Modules.PlotManagement;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.danidipp.dippgen.Plugin;
import com.danidipp.dippgen.Modules.PlotManagement.PlotDeed.DEED_TYPE;

public class PlotClaimGUI {
	public static InventoryHolder plotClaimInventoryHolder = new InventoryHolder() {
		@Override
		public Inventory getInventory() {
			return null;
		}
	};

	public static Inventory create(Plot plot, Player player) {
		var plots = Plot.getOwnedPlots(player);
		var personalPlotLimit = player.hasPermission("dipp.admin") ? plots.size() + 1 : Plot.getPlotLimit(player);
		var maxUnlockablePlots = 4;
		var inventoryRows = (int) Math.ceil(Math.max(personalPlotLimit, maxUnlockablePlots) / 9.0);
		var inventory = Bukkit.createInventory(PlotClaimGUI.plotClaimInventoryHolder, inventoryRows * 9,
				"§6Choose a plot slot (" + plots.size() + "/" + personalPlotLimit + " used)");
		int slot = 0;
		for (var ownedPlot : plots) {
			var district = ownedPlot.district();
			var iconName = district.deed().name() + ": " + ownedPlot.getName();
			var iconLore = List.of("§7" + district.name());
			var iconItem = ItemUtil.generateIcon(Material.RABBIT_FOOT, iconName, iconLore, district.deed().getCustomModelData(), ownedPlot);
			inventory.setItem(slot, iconItem);
			slot++;
		}
		for (var i = slot; i < personalPlotLimit; i++) {
			var iconItem = ItemUtil.generateIcon(Material.WHITE_STAINED_GLASS_PANE, "§2Empty Slot", List.of("§7Click here to claim this plot"), 0,
					plot);
			inventory.setItem(i, iconItem);
		}
		for (var i = personalPlotLimit; i < 4; i++) {
			var iconItem = ItemUtil.generateIcon(Material.RED_STAINED_GLASS_PANE, "§4Locked Slot",
					List.of("§7Gain additional plot slots from your skill trees"), 0, plot);
			inventory.setItem(i, iconItem);
		}
		return inventory;
	}

	public static Listener listener = new Listener() {
		@EventHandler
		public void onInventoryClick(InventoryClickEvent event) {
			if (event.getInventory().getHolder() != PlotClaimGUI.plotClaimInventoryHolder)
				return;
			event.setCancelled(true);

			if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != PlotClaimGUI.plotClaimInventoryHolder
					|| event.getCurrentItem() == null)
				return;

			var item = event.getCurrentItem();

			switch (item.getType()) {
			case RABBIT_FOOT:
				this.clickExistingPlot(event);
				break;

			case WHITE_STAINED_GLASS_PANE:
				this.clickNewPlot(event);
				break;

			case RED_STAINED_GLASS_PANE:
				this.clickLockedSlot(event);
				break;

			default:
				Plugin.plugin.getLogger().warning("Unknown item type in PlotClaimInventory: " + item.getType());
				break;
			}
		}

		private void clickExistingPlot(InventoryClickEvent event) {
			Player player = (Player) event.getWhoClicked();
			player.playSound(player, "lom:fail_wrong", 0.5f, 1.0f);
		}

		private void clickNewPlot(InventoryClickEvent event) {
			Player player = (Player) event.getWhoClicked();
			ItemStack item = event.getCurrentItem();
			ItemMeta meta = item.getItemMeta();
			String plotId = meta.getPersistentDataContainer().get(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING);
			Plot plot = Plot.getPlot(plotId);
			if (plot == null) {
				player.sendMessage("error: Plot \"" + plotId + "\" not found. Please tell Dani!");
				Bukkit.getScheduler().runTask(Plugin.plugin, () -> event.getView().close());
				return;
			}
			if (plot.region().getOwners().size() > 0) {
				player.sendMessage("error: This plot is already claimed. If you believe this is an error, please tell Dani!");
				return;
			}
			plot.region().getOwners().addPlayer(player.getUniqueId());
			player.playSound(player, "lom:light.buff2", 0.5f, 1.0f);

			ItemStack deedItem = PlotDeed.getDeedItem(plot, DEED_TYPE.MANAGEMENT);
			player.getInventory().setItem(player.getInventory().getHeldItemSlot(), deedItem);
			Bukkit.getScheduler().runTask(Plugin.plugin, () -> player.openInventory(PlotManagementGUI.create(plot, player)));
		}

		private void clickLockedSlot(InventoryClickEvent event) {
			Player player = (Player) event.getWhoClicked();
			player.playSound(player, "lom:fail_wrong", 0.5f, 1.0f);
		}

	};
}

class ItemUtil {
	public static ItemStack generateIcon(Material material, String name, List<String> lore, int customModelData, Plot plot) {
		ItemStack icon = new ItemStack(material);
		ItemMeta meta = icon.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(lore);
		meta.setCustomModelData(customModelData);
		meta.getPersistentDataContainer().set(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING, plot.getId());
		icon.setItemMeta(meta);
		return icon;
	}
}