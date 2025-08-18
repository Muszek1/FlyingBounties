package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.YamlUpdater;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import org.bukkit.configuration.file.FileConfiguration;
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
		return "/playerbounty reload";
	}

	@Override
	public String getPermission() {
		return "zlecenia.reload";
	}

	@Override
	public void perform(Player player, String[] args) {
		PlayerBounty plugin = PlayerBounty.getInstance();
		// Reload config.yml with defaults
		YamlUpdater updater = new YamlUpdater(plugin);
		FileConfiguration config = updater.update("config.yml");

		// Reload languages
		FileConfiguration langEn = updater.update("langs/en.yml");
		FileConfiguration langPl = updater.update("langs/pl.yml");
		String locale = config.getString("settings.locale", "pl").toLowerCase();
		FileConfiguration langConfig = locale.equals("en") ? langEn : langPl;

		// Reload settings (which also reloads ConfigKey via YamlUpdater inside)
		Settings.load(langConfig);

		// Debug: log new stacked value
		plugin.getLogger().info("[DEBUG] After reload, Bounty.Stacked = " + Settings.ConfigKey.BOUNTY_STACKED.get());

		// Send confirmation
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
