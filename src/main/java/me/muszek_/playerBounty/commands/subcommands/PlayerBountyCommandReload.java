package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.listeners.BountyMenuListener;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerBountyCommandReload extends SubCommand {

	@Override
	public String getName() {
		return "reload";
	}

	@Override
	public String getDescription() {
		return "Reloads plugin config and language files";
	}

	@Override
	public String getSyntax() {
		return "/bounty reload";
	}

	@Override
	public String getPermission() {
		return "flyingbounties.reload";
	}

	@Override
	public void perform(Player player, String[] args) {
		Settings.load();

		new BountyMenuListener(PlayerBounty.getInstance()).reloadMenuConfig();

		String msg = Settings.LangKey.PLUGIN_RELOADED.get();
		for (String line : msg.split("\n")) {
			player.sendMessage(Colors.color(line));
		}
	}

	@Override
	public List<String> getSubcommandArguments(Player player, String[] args) {
		return null;
	}
}