package com.danidipp.dippgen.Modules.PlotManagement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.danidipp.dippgen.Plugin;

public record PlotDeed(String name, String firstLore, int getCustomModelData) {
	public static enum DEED_TYPE {
		CLAIM, MANAGEMENT, OPEN
	}

	public static NamespacedKey PLOT_ID_KEY = new NamespacedKey(Plugin.plugin, "plot-id");

	public static Set<PlotDeed> deeds = District.districts.stream().map(d -> d.deed()).collect(Collectors.toSet());

	@Nullable
	public static DEED_TYPE getType(ItemMeta meta) {
		if (meta == null || !meta.hasDisplayName() || !meta.hasLore() || !meta.hasCustomModelData())
			return null;

		if (!District.districts.stream().map(d -> d.deed()).anyMatch(d -> {
			var nameMatch = meta.getDisplayName().startsWith(d.name());
			var loreMatch = true; //d.firstLore().equals(meta.getLore().get(0));
			var modelDataMatch = meta.getCustomModelData() == d.getCustomModelData();
			return nameMatch && loreMatch && modelDataMatch;
		}))
			return null;

		if (meta.getDisplayName().contains("[OPEN]"))
			return DEED_TYPE.OPEN;
		if (meta.getDisplayName().contains(": "))
			return DEED_TYPE.MANAGEMENT;
		return DEED_TYPE.CLAIM;
	}

	public static ItemStack getDeedItem(Plot plot, DEED_TYPE type) {
		var item = new ItemStack(Material.RABBIT_FOOT);
		var meta = item.getItemMeta();

		var district = District.districts.stream().filter(d -> d.id().equals(plot.region().getId().split("-")[0])).findFirst().orElse(null);
		if (district == null)
			return null;
		var deed = district.deed();

		meta.setCustomModelData(deed.getCustomModelData);

		switch (type) {
		case CLAIM:
			meta.setDisplayName(deed.name);
			meta.setLore(List.of(deed.firstLore));
			break;
		case MANAGEMENT:
			var ownerName = plot.region().getOwners().getUniqueIds().stream().map(uuid -> Plugin.plugin.getServer().getOfflinePlayer(uuid))
					.map(OfflinePlayer::getName).collect(Collectors.joining(", "));
			meta.setDisplayName(deed.name + ": " + plot.region().getId().split("-")[1]);
			meta.setLore(List.of("§eOwner: §6" + ownerName, "§7Open to manage your plot"));
			break;
		case OPEN:
			meta.setDisplayName(deed.name);
			meta.setLore(List.of("Dani forgot to update this!"));
			break;
		}
		meta.getPersistentDataContainer().set(PLOT_ID_KEY, PersistentDataType.STRING, plot.region().getId());
		item.setItemMeta(meta);
		return item;
	}

	public static Listener listener = new Listener() {
		@EventHandler
		public void onPlayerInteract(PlayerInteractEvent event) {
			if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK
					|| event.getHand() == EquipmentSlot.OFF_HAND)
				return;

			var item = event.getItem();
			if (item == null || item.getType() != Material.RABBIT_FOOT)
				return;

			var deedType = PlotDeed.getType(item.getItemMeta());
			if (deedType == null)
				return;

			event.setCancelled(true);

			var player = event.getPlayer();
			var plot = Plot.getPlot(player.getLocation());
			if (plot == null) {
				player.sendMessage("error: You are not in a plot");
				return;
			}

			switch (deedType) {
			case CLAIM:
				var plotDistrict = District.districts.stream().filter(d -> d.id().equals(plot.region().getId().split("-")[0])).findFirst()
						.orElse(null);
				var deedDistrict = District.districts.stream().filter(d -> d.deed().name.equals(item.getItemMeta().getDisplayName())).findFirst()
						.orElse(null);
				if (plotDistrict == null || deedDistrict == null) {
					player.sendMessage("error: You can't use this deed here");
					return;
				}

				if (plot.region().getOwners().size() > 0) {
					player.sendMessage("error: This plot is already claimed");
					return;
				}

				if (plotDistrict != deedDistrict) {
					player.sendMessage("error: This deed is not for this district");
					return;
				}
				player.openInventory(PlotClaimGUI.create(plot, player));
				break;
			case MANAGEMENT:
				var plotId = item.getItemMeta().getDisplayName().split(": ")[1];
				if (!plot.region().getId().endsWith(plotId)) {
					player.sendMessage("error: This deed is not for this plot");
					return;
				}
				var isOwner = plot.region().getOwners().contains(player.getUniqueId());
				var isMember = plot.region().getMembers().contains(player.getUniqueId());
				if (!isOwner && !isMember) {
					player.sendMessage("error: You are not a member of this plot");
					return;
				}
				player.openInventory(PlotManagementGUI.create(plot, player));
				break;
			case OPEN:
				player.openInventory(PlotClaimGUI.create(plot, player));
				break;
			}
		}
	};
}
