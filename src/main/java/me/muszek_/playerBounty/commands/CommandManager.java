package me.muszek_.playerBounty.commands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.subcommands.*;
import me.muszek_.playerBounty.menusystem.BountyMenu;
import me.muszek_.playerBounty.settings.Settings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

	private final ArrayList<SubCommand> subCommands = new ArrayList<>();
	private final BountyMenu menu;

	public CommandManager(PlayerBounty plugin) {
		this.menu = new BountyMenu(plugin);

		subCommands.add(new PlayerBountyCommandCreate());
		subCommands.add(new PlayerBountyCommandHelp());
		subCommands.add(new PlayerBountyCommandReload());
		subCommands.add(new PlayerBountyCommandRemove());
		subCommands.add(new PlayerBountyCommandMenu(plugin));
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (!(sender instanceof Player player)) {
			if (args.length > 0) {
				for (SubCommand subCommand : getSubCommands()) {
					if (args[0].equalsIgnoreCase(subCommand.getName())) {
						if (!sender.hasPermission(subCommand.getPermission())) {
							sender.sendMessage(Colors.color(Settings.LangKey.NO_PERMISSION.get()));
							return true;
						}
						sender.sendMessage(Colors.color("&cThis command must be run by a player."));
						return true;
					}
				}
			}
			sender.sendMessage(Colors.color(Settings.LangKey.PLAYER_NOT_FOUND.get()));
			return true;
		}

		if (args.length == 0) {
			if (!player.hasPermission("flyingbounties.menu")) {
				player.sendMessage(Colors.color(Settings.LangKey.NO_PERMISSION.get()));
				return true;
			}
			menu.open(player);
			return true;
		}

		for (SubCommand subCommand : getSubCommands()) {
			if (args[0].equalsIgnoreCase(subCommand.getName())) {

				if (!player.hasPermission(subCommand.getPermission())) {
					player.sendMessage(Colors.color(Settings.LangKey.NO_PERMISSION.get()));
					return true;
				}

				subCommand.perform(player, args);
				return true;
			}
		}

		player.sendMessage(Colors.color("&c> Unknown subcommand. Use /bounty help."));
		return true;
	}

	public ArrayList<SubCommand> getSubCommands() {
		return subCommands;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

		if (args.length == 1) {
			ArrayList<String> subCommandsArguments = new ArrayList<>();

			for (SubCommand sc : getSubCommands()) {
				if (sender.hasPermission(sc.getPermission())) {
					subCommandsArguments.add(sc.getName());
				}
			}
			return subCommandsArguments;
		} else if (args.length >= 2) {
			for (int i = 0; i < getSubCommands().size(); i++) {
				SubCommand sc = getSubCommands().get(i);
				if (args[0].equalsIgnoreCase(sc.getName())) {
					if (sender instanceof Player) {
						return sc.getSubcommandArguments((Player) sender, args);
					} else {
						return null;
					}
				}
			}
		}

		return null;
	}
}