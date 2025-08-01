package me.muszek_.playerBounty.commands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.subcommands.PlayerBountyCommandCreate;
import me.muszek_.playerBounty.commands.subcommands.PlayerBountyCommandHelp;
import me.muszek_.playerBounty.commands.subcommands.PlayerBountyCommandMenu;
import me.muszek_.playerBounty.commands.subcommands.PlayerBountyCommandReload;
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

	private final PlayerBounty plugin;
	private final ArrayList<SubCommand> subCommands = new ArrayList<>();

	public CommandManager(PlayerBounty plugin) {
		this.plugin = plugin;
		subCommands.add(new PlayerBountyCommandCreate());
		subCommands.add(new PlayerBountyCommandHelp());
		subCommands.add(new PlayerBountyCommandReload());
		subCommands.add(new PlayerBountyCommandMenu(plugin));
	}


	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		if (sender instanceof Player player) {
			if (args.length > 0) {
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
				player.sendMessage(Colors.color("&cUnknown sub-command. Use /playerbounty help."));
			} else {
				player.sendMessage(Colors.color("Usage: /playerbounty help"));
			}
		}
		return true;
	}

	public ArrayList<SubCommand> getSubCommands() {
		return subCommands;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

		if (args.length == 1) {
			ArrayList<String> subCommandsArguments = new ArrayList<>();

			for (int i = 0; i < getSubCommands().size(); i++) {
				subCommandsArguments.add(getSubCommands().get(i).getName());
			}
			return subCommandsArguments;
		} else if (args.length >= 2) {
			for (int i = 0; i < getSubCommands().size(); i++) {
				if (args[0].equalsIgnoreCase(getSubCommands().get(i).getName())) {
					return getSubCommands().get(i).getSubcommandArguments((Player) sender, args);
				}
			}
		}


		return null;
	}
}
