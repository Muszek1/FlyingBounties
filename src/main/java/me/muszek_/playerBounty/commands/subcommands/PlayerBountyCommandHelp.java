package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerBountyCommandHelp extends SubCommand {


	@Override
	public String getName() {
		return "pomoc";
	}

	@Override
	public String getDescription() {
		return "displays the help message";
	}

	@Override
	public String getSyntax() {
		return "/zlecenia help";
	}

	@Override
	public void perform(Player player, String[] args) {


		player.sendMessage(Colors.color(Settings.LangKey.HELP.get()));

	}

	@Override
	public String getPermission() {
		return "playerbounty.pomoc";
	}

	@Override
	public List<String> getSubcommandArguments(Player player, String[] args) {
		return List.of();
	}
}
