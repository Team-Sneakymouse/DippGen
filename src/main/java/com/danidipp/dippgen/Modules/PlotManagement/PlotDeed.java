package com.danidipp.dippgen.Modules.PlotManagement;

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
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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

		var itemName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());

		var persistentData = meta.getPersistentDataContainer();
		if (persistentData.has(PLOT_ID_KEY, PersistentDataType.STRING)) {
			if (itemName.contains("[OPEN]"))
				return DEED_TYPE.OPEN;
			if (itemName.contains(": "))
				return DEED_TYPE.MANAGEMENT;
		}

		if (District.districts.stream().map(d -> d.deed()).anyMatch(d -> {
			var nameMatch = itemName.startsWith(d.name()) || itemName.startsWith("Stick District");
			var loreMatch = true; //d.firstLore().equals(meta.getLore().get(0));
			var modelDataMatch = meta.getCustomModelData() == d.getCustomModelData();
			return nameMatch && loreMatch && modelDataMatch;
		}))
			return DEED_TYPE.CLAIM;

		return null;
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
				meta.displayName(Component.text(deed.name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				meta.lore(List.of(Component.text(deed.firstLore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				break;
			case MANAGEMENT:
				var ownerName = plot.region().getOwners().getUniqueIds().stream().map(uuid -> Plugin.plugin.getServer().getOfflinePlayer(uuid))
						.map(OfflinePlayer::getName).collect(Collectors.joining(", "));
				if (ownerName.isEmpty())
					ownerName = "n.a.";
				meta.displayName(Component.text(deed.name + ": " + plot.region().getId().split("-")[1], NamedTextColor.GOLD)
						.decoration(TextDecoration.ITALIC, false));
				meta.lore(List.of(
						Component.text("Owner: ", NamedTextColor.YELLOW).append(Component.text(ownerName, NamedTextColor.GOLD))
								.decoration(TextDecoration.ITALIC, false),
						Component.text("Open to manage your plot", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
				break;
			case OPEN:
				meta.displayName(Component.text(deed.name, NamedTextColor.GOLD));
				meta.lore(List.of(Component.text("Dani forgot to update this!", NamedTextColor.GRAY)));
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
			var itemName = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
			// Plugin.plugin.getLogger().info("Item name: " + itemName);

			var deedType = PlotDeed.getType(item.getItemMeta());
			if (deedType == null)
				return;

			event.setCancelled(true);

			var player = event.getPlayer();
			var currentPlot = Plot.getPlot(player.getLocation());
			if (currentPlot == null) {
				player.sendMessage("error: You are not in a plot");
				return;
			}

			switch (deedType) {
				case CLAIM:
					var currentPlotDistrictId = currentPlot.region().getId().split("-")[0];
					var plotDistrict = District.districts.stream().filter(d -> d.id().equals(currentPlotDistrictId)).findFirst().orElse(null);
					var deedDistrict = District.districts.stream().filter(d -> d.deed().name.equals(itemName)).findFirst().orElse(null);
					if (plotDistrict == null || deedDistrict == null) {
						player.sendMessage("error: You can't use this deed here");
						return;
					}

					if (currentPlot.region().getOwners().size() > 0) {
						player.sendMessage("error: This plot is already claimed");
						return;
					}

					if (plotDistrict != deedDistrict) {
						player.sendMessage("error: This deed is not for this district");
						return;
					}
					player.openInventory(PlotClaimGUI.create(currentPlot, player));
					break;
				case MANAGEMENT:
					var persistentData = item.getItemMeta().getPersistentDataContainer();
					var plotId = persistentData.get(PLOT_ID_KEY, PersistentDataType.STRING);
					if (plotId == null) {
						player.sendMessage("error: This management deed does not have a plot id. Please tell Dani!");
						return;
					}
					if (!currentPlot.region().getId().endsWith(plotId)) {
						player.sendMessage("error: This deed is not for this plot");
						return;
					}
					var wgPlayer = WorldGuardPlugin.inst().wrapOfflinePlayer(player);
					var isOwner = currentPlot.region().getOwners().contains(wgPlayer);
					var isMember = currentPlot.region().getMembers().contains(wgPlayer);
					if (!isOwner && !isMember) {
						player.sendMessage("error: You are not a member of this plot");
						return;
					}
					player.openInventory(PlotManagementGUI.create(currentPlot, player));
					break;
				case OPEN:
					player.openInventory(PlotClaimGUI.create(currentPlot, player));
					break;
			}
		}
	};
}
