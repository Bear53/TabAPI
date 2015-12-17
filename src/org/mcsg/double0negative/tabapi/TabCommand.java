package org.mcsg.double0negative.tabapi;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

public class TabCommand implements CommandExecutor {

	TabAPI plugin;

	public TabCommand(TabAPI pl) {
		this.plugin = pl;
	}

	public boolean pOnline(Player p) {
		if (!p.isOnline()) {
			return false;
		} else {
			return true;
		}
	}

	public boolean onCommand(CommandSender sender, Command cmd1,
			String commandLabel, String[] args) {
		PluginDescriptionFile pdfFile = plugin.getDescription();
		Player player = null;
		if ((sender instanceof Player)) {
			player = (Player) sender;
			if ((args.length == 1) && (player.hasPermission("tabapi.reload"))) {
				plugin.reloadConfiguration();
				TabAPI.updateAll();
			} else {
				player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD
						+ "TabAPI - Bear53" + ChatColor.RESET + ChatColor.RED
						+ " Version: " + pdfFile.getVersion());
			}
		} else {
			sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD
					+ "TabAPI - Bear53" + ChatColor.RESET + ChatColor.RED
					+ " Version: " + pdfFile.getVersion());
			return true;
		}
		return true;
	}
}
