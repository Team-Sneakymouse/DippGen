package com.danidipp.dippgen.Modules.PlotManagement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import com.danidipp.dippgen.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PlotManagementGUI {
	public static InventoryHolder plotManagementInventoryHolder = new InventoryHolder() {
		@Override
		public org.bukkit.inventory.Inventory getInventory() {
			return null;
		}
	};

	public static int getMemberLimit(@NotNull OfflinePlayer player) {
		if (!player.isConnected()) return 1;
		var maxMembersString = PlaceholderAPI.setPlaceholders(player, "%ms_variable_statPlotFriends%");
		try {
			return (int) Float.parseFloat(maxMembersString);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	public static Inventory create(Plot plot, Player player) {
		var plotName = plot.region().getId().split("-")[1];
		var maxMembers = Math.min(getMemberLimit(player), 5 * 9); // hard limit due to inventory size
		var rows = (9 + (1 + maxMembers / 9) * 9);
		var inventory = Bukkit.createInventory(PlotManagementGUI.plotManagementInventoryHolder, rows,
				Component.text("Plot Management: " + plotName, NamedTextColor.GOLD));
		inventory.setItem(0, IconUtil.plotOwner(plot));
		inventory.setItem(1, IconUtil.plotInfo(plot));
		// inventory.setItem(7, IconUtil.lockToggle(plot));
		var isOwner = plot.region().getOwners().getUniqueIds().contains(player.getUniqueId());
		if (isOwner) inventory.setItem(8, IconUtil.abandonPlot(plot));

		var uuids = plot.region().getMembers().getUniqueIds().stream().toList();
		for (var i = 0; i < uuids.size(); i++) {
			inventory.setItem(9 + i, IconUtil.memberPortrait(Bukkit.getOfflinePlayer(uuids.get(i)), plot, isOwner));
		}
		if (plot.isManager(player))
			for (var i = uuids.size(); i < getMemberLimit(player); i++) {
				inventory.setItem(9 + i, IconUtil.addMember(plot));
			}

		return inventory;
	}

	public static Listener listener = new Listener() {
		private Map<Player, PlayerAddMemberInfo> addMemberTimers = new HashMap<Player, PlayerAddMemberInfo>();

		@EventHandler
		public void onInventoryClick(InventoryClickEvent event) {
			if (event.getInventory().getHolder() != PlotManagementGUI.plotManagementInventoryHolder)
				return;
			event.setCancelled(true);

			if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != PlotManagementGUI.plotManagementInventoryHolder
					|| event.getCurrentItem() == null)
				return;

			if (!(event.getWhoClicked() instanceof Player)) {
				event.getWhoClicked().sendMessage("what are you??");
				return;
			}

			var item = event.getCurrentItem();
			var slot = event.getSlot();
			var player = (Player) event.getWhoClicked();

			String plotId = item.getItemMeta().getPersistentDataContainer().get(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING);
			Plot plot = Plot.getPlot(plotId);
			if (plot == null) {
				player.sendMessage("error: Plot \"" + plotId + "\" not found. Please tell Dani!");
				Bukkit.getScheduler().runTask(Plugin.plugin, () -> event.getView().close());
				return;
			}

			var isOwner = plot.region().getOwners().getUniqueIds().contains(player.getUniqueId());
			var isManager = plot.isManager(player);
			// if (slot == 7) {
			// 	this.toggleLock(event, plot);
			// 	return;
			// }
			if (slot == 8) {
				if (isOwner) this.abandonPlot(event, plot);
				return;
			}
			if (item.getType() == Material.PLAYER_HEAD && slot >= 9) {
				if (isManager) this.removeMember(event, plot);
				return;
			}
			if (item.getType() == Material.GREEN_STAINED_GLASS_PANE) {
				if (isManager) this.addMember(event, plot);
				return;
			}
		}

		// void toggleLock(InventoryClickEvent event, Plot plot) {
		// 	var unlockedFlag = plot.region().getFlag(Plot.plotUnlockedFlag);
		// 	var isUnlocked = unlockedFlag == null ? false : unlockedFlag.booleanValue();

		// 	plot.region().setFlag(Plot.plotUnlockedFlag, !isUnlocked);

		// 	event.getClickedInventory().setItem(8, IconUtil.lockToggle(plot));
		// }

		void abandonPlot(InventoryClickEvent event, Plot plot) {
			var player = (Player) event.getWhoClicked();

			plot.region().getMembers().removeAll();
			plot.region().getOwners().removeAll();
			plot.region().setFlag(Plot.teleportLocationFlag, null);
			plot.region().setFlag(Plot.plotUnlockedFlag, false);

			player.getInventory().setItemInMainHand(null);
			player.playSound(player, "lom:light.buff2", 0.5f, 1.0f);
			Bukkit.getScheduler().runTask(Plugin.plugin, () -> player.closeInventory());
		}

		void addMember(InventoryClickEvent event, Plot plot) {
			var player = (Player) event.getWhoClicked();
			var playerName = player.getName();
			var bossbarCommand = "cmi bossbarmsg " + playerName + " &aTouch someone to add them to this plot -sec:-20 -n:addmember-" + plot.getId();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bossbarCommand);
			Bukkit.getScheduler().runTask(Plugin.plugin, () -> player.closeInventory());
			var task = Bukkit.getScheduler().runTaskLater(Plugin.plugin, () -> this.addMemberTimers.remove(player), 20 * 20);
			this.addMemberTimers.put(player, new PlayerAddMemberInfo(plot, task));
		}

		void removeMember(InventoryClickEvent event, Plot plot) {
			var item = event.getCurrentItem();
			var player = ((SkullMeta) item.getItemMeta()).getOwningPlayer();
			plot.region().getMembers().removePlayer(player.getUniqueId());
			Plugin.plugin.getLogger().info("removed member");

			Bukkit.getScheduler().runTask(Plugin.plugin,
					() -> event.getWhoClicked().openInventory(PlotManagementGUI.create(plot, (Player) event.getWhoClicked())));
		}

		@EventHandler
		void onPlayerHitEvent(EntityDamageByEntityEvent event) {
			if (event.getDamager().getType() != EntityType.PLAYER || event.getEntity().getType() != EntityType.PLAYER)
				return;
			var player = (Player) event.getDamager();
			var target = (Player) event.getEntity();

			if (!this.addMemberTimers.containsKey(player))
				return;
			Plugin.plugin.getLogger().info("contains");

			var info = this.addMemberTimers.get(player);
			info.task().cancel();
			this.addMemberTimers.remove(player);
			var bossbarCommand = "cmi bossbarmsg " + player.getName() + " -cancel:addmember-" + info.plot().getId();
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), bossbarCommand);

			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getPositionalPlaySoundCommand("lom:cute.hit", player, target.getLocation()));
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getPositionalPlaySoundCommand("lom:cute.hit", target, player.getLocation()));

			info.plot().region().getMembers().addPlayer(target.getUniqueId());
			player.openInventory(PlotManagementGUI.create(info.plot(), player));
		}

		String getPositionalPlaySoundCommand(String sound, Player listener, Location location) {
			var playerName = listener.getName();
			var x = location.getX();
			var y = location.getY();
			var z = location.getZ();
			return "playsound " + sound + " master " + playerName + " " + x + " " + y + " " + z;
		}
	};
}

class IconUtil {
	static ItemStack plotOwner(Plot plot) {
		var item = new ItemStack(Material.PLAYER_HEAD);
		var meta = (SkullMeta) item.getItemMeta();

		var ownerUUID = plot.region().getOwners().getUniqueIds().stream().findFirst().orElse(null);
		var owner = ownerUUID != null ? Bukkit.getOfflinePlayer(ownerUUID) : null;
		var ownerName = owner != null ? owner.getName() : "No Owner";
		var ownedPlots = Plot.getOwnedPlots(ownerUUID);
		meta.setOwningPlayer(owner);

		var titleComponent = Component.text("Plot Owner", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
		var nameComponent = Component.text(ownerName, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false);
		var plotCountComponent = Component.text("Plot slots used: ", NamedTextColor.WHITE)
				.append(Component.text(ownedPlots.size(), NamedTextColor.YELLOW))
				.append(Component.text("/", NamedTextColor.GRAY))
				.append(Component.text(Plot.getPlotLimit(owner), NamedTextColor.YELLOW))
				.decoration(TextDecoration.ITALIC, false);
		meta.displayName(titleComponent);
		meta.lore(List.of(nameComponent, plotCountComponent));
		meta.getPersistentDataContainer().set(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING, plot.getId());
		item.setItemMeta(meta);
		return item;
	}

	static ItemStack plotInfo(Plot plot) {
		var item = new ItemStack(Material.BOOK);
		var meta = item.getItemMeta();

		var memberCount = plot.region().getMembers().size();
		var uuid = plot.region().getOwners().getUniqueIds().stream().findFirst().orElse(null);
		var maxMembers = uuid == null ? 0 : PlotManagementGUI.getMemberLimit(Bukkit.getOfflinePlayer(uuid));
		//var unlockedFlag = plot.region().getFlag(Plot.plotUnlockedFlag);
		//var isUnlocked = unlockedFlag != null ? unlockedFlag.booleanValue() : false;

		var districtLore = Component
				.text("District: ", NamedTextColor.WHITE)
				.append(Component.text(plot.district().name(), NamedTextColor.GOLD))
				.decoration(TextDecoration.ITALIC, false);
		var membersLore = Component
				.text("Members: ", NamedTextColor.WHITE)
				.append(Component.text(memberCount, NamedTextColor.GOLD))
				.append(Component.text("/", NamedTextColor.GRAY))
				.append(Component.text(maxMembers, NamedTextColor.GOLD))
				.decoration(TextDecoration.ITALIC, false);
		// var lockLore = "§fLock Status: " + (isUnlocked ? "§cUnlocked" : "§5Locked");
		// var lockLore2 = "§7While the deed is unlocked, you can throw it to";
		// var lockLore3 = "§7another player to transfer plot ownership to them.";
		meta.displayName(Component.text("Plot Info", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
		meta.lore(List.of(districtLore, membersLore/*, lockLore, lockLore2, lockLore3 */));
		meta.getPersistentDataContainer().set(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING, plot.getId());
		item.setItemMeta(meta);
		return item;
	}

	// static ItemStack lockToggle(Plot plot) {
	// 	var item = new ItemStack(Material.JIGSAW);
	// 	var meta = item.getItemMeta();

	// 	var unlockedFlag = plot.region().getFlag(Plot.plotUnlockedFlag);
	// 	var lock = unlockedFlag != null ? unlockedFlag.booleanValue() : false;

	// 	var toggleName = lock ? Component.text("Deed Unlocked", NamedTextColor.RED) : Component.text("Deed Locked", NamedTextColor.AQUA);
	// 	var toggleLore = lock
	// 			? List.of(Component.text("Click here to lock the deed again", NamedTextColor.YELLOW))
	// 			: List.of(
	// 					Component.text("Click to unlock this deed", NamedTextColor.YELLOW),
	// 					Component.text("Give the unlocked deed to someone", NamedTextColor.GRAY),
	// 					Component.text("else to transfer the plot to them."));

	// 	meta.displayName(toggleName);
	// 	meta.lore(toggleLore);
	// 	meta.setCustomModelData(lock ? 235 : 234);
	// 	meta.getPersistentDataContainer().set(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING, plot.getId());
	// 	item.setItemMeta(meta);
	// 	return item;
	// }

	static ItemStack abandonPlot(Plot plot) {
		var item = new ItemStack(Material.BARRIER);
		var meta = item.getItemMeta();

		var titleComponent = Component.text("Abandon Plot", NamedTextColor.RED)
				.decoration(TextDecoration.ITALIC, false);
		var loreComponents = List.of(
				Component.text("Click to abandon this plot", NamedTextColor.YELLOW),
				Component.text("This will remove all members and", NamedTextColor.GRAY),
				Component.text("make the plot available for purchase.", NamedTextColor.GRAY));
		loreComponents = loreComponents.stream().map(component -> component.decoration(TextDecoration.ITALIC, false)).toList();

		meta.displayName(titleComponent);
		meta.lore(loreComponents);
		meta.getPersistentDataContainer().set(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING, plot.getId());
		item.setItemMeta(meta);
		return item;
	}

	static ItemStack memberPortrait(OfflinePlayer player, Plot plot, boolean isOwner) {
		var item = new ItemStack(Material.PLAYER_HEAD);
		var meta = (SkullMeta) item.getItemMeta();

		if (player.getName() != null) {
			meta.setOwningPlayer(player);
		}
		meta.displayName(Component.text(player.getName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		if (isOwner) {
			var loreComponents = List.of(
					Component.text("Click to remove them from the plot", NamedTextColor.YELLOW),
					Component.text("Additional member slots can be", NamedTextColor.GRAY),
					Component.text("acquired from Skill Trees", NamedTextColor.GRAY));
			loreComponents = loreComponents.stream().map(component -> component.decoration(TextDecoration.ITALIC, false)).toList();
			meta.lore(loreComponents);
		}
		meta.getPersistentDataContainer().set(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING, plot.getId());
		item.setItemMeta(meta);
		return item;
	}

	static ItemStack addMember(Plot plot) {
		var item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
		var meta = item.getItemMeta();

		meta.displayName(Component.text("Add Member", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		meta.lore(List.of(
				Component.text("Additional member slots can be", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text("acquired from Skill Trees", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
		meta.getPersistentDataContainer().set(PlotDeed.PLOT_ID_KEY, PersistentDataType.STRING, plot.getId());

		item.setItemMeta(meta);
		return item;
	}

}

record PlayerAddMemberInfo(Plot plot, BukkitTask task) {};
