package com.danidipp.dippgen.Commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.danidipp.dippgen.Plugin;

public class ShowBookCommand implements ICommandImpl {

	@Override
	public String getName() {
		return "showbook";
	}

	@Override
	public CommandExecutor getExecutor() {
		return new CommandExecutor() {
			@Override
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
				if (args.length == 0) return false;

				if (!(sender instanceof Player)) {
					sender.sendMessage("Only players can use this command");
					return true;
				}
				var player = (Player) sender;

				var bookId = args[0];
				BookMeta bookMeta = Plugin.plugin.recentBooks.get(bookId);
				if (bookMeta == null) {
					sender.sendMessage("Error - Book lookup expired");
				}

				ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
				book.setItemMeta(bookMeta);
				player.openBook(book);
				return true;
			}
		};
	}

	@Override
	public TabCompleter getTabCompleter() {
		return null;
	}

}
