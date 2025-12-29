package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerBountyCommandHelp extends SubCommand {

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public String getDescription() {
		return "Displays the help message";
	}

	@Override
	public String getSyntax() {
		return "/bounty help";
	}

	@Override
	public void perform(Player player, String[] args) {
		player.sendMessage(Colors.color(Settings.LangKey.HELP.get()));
	}

	@Override
	public String getPermission() {
		return "flyingbounties.help";
	}

	@Override
	public List<String> getSubcommandArguments(Player player, String[] args) {
		return List.of();
	}
}