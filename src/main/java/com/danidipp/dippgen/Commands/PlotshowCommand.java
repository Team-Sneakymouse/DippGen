package com.danidipp.dippgen.Commands;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;

// Displays all plots owned by a player
public class PlotshowCommand implements ICommandImpl {
	public String getName() {
		return "plotshow";
	}

	@Override
	public CommandExecutor getExecutor() {
		return new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
				if (args.length == 0) { // No player name provided
					return false;
				}

				String playerName = args[0];
				OfflinePlayer player = List.of(Bukkit.getOfflinePlayers()).stream()
						.filter(p -> p.getName().toLowerCase().equals(playerName.toLowerCase())).findFirst().orElse(null);

				if (player == null) {
					sender.sendMessage("error: Player " + playerName + " does not exist");
					return true;
				}

				// Get all regions
				World world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
				var wgWorld = BukkitAdapter.adapt(world);
				RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
				Collection<ProtectedRegion> regions = regionContainer.get(wgWorld).getRegions().values();

				// Filter regions by owner
				regions = regions.stream().filter(r -> r.getOwners().contains(player.getUniqueId())).toList();

				// Display regions
				sender.sendMessage(playerName + " owns " + regions.size() + " plots:");
				for (var region : regions) {
					var regionCenter = region.getMinimumPoint().add(region.getMaximumPoint()).divide(2);
					var centerY = region.getMaximumPoint().getBlockY();
					while (world.getBlockAt(regionCenter.getBlockX(), centerY, regionCenter.getBlockZ()).isPassable()) {
						centerY--;
					}
					regionCenter = regionCenter.withY(centerY + 1);

					var infoComponent = new TextComponent("[info]");
					infoComponent.setColor(ChatColor.YELLOW);
					infoComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Show plot info")));
					infoComponent
							.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rg info -w " + world.getName() + " " + region.getId()));

					var teleportComponent = new TextComponent("[tp]");
					teleportComponent.setColor(ChatColor.YELLOW);
					teleportComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Teleport to plot")));
					teleportComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
							"/minecraft:tp @s " + regionCenter.getBlockX() + " " + regionCenter.getBlockY() + " " + regionCenter.getBlockZ()));

					var text = new TextComponent("- " + region.getId() + " ");
					text.addExtra(infoComponent);

					if (sender instanceof Player) {
						text.addExtra(" ");
						text.addExtra(teleportComponent);
					}

					sender.spigot().sendMessage(text);
				}

				return true;
			}
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return new TabCompleter() {
			@Override
			public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
				var playerlist = List.of(Bukkit.getOfflinePlayers()).stream().filter(p -> p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
						.map(p -> p.getName()).collect(Collectors.toList());
				return playerlist;
			}
		};
	}
}
