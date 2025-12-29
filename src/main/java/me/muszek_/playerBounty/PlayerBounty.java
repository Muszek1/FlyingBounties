package me.muszek_.playerBounty;

import me.muszek_.playerBounty.commands.CommandManager;
import me.muszek_.playerBounty.listeners.BountyCompleteListener;
import me.muszek_.playerBounty.listeners.BountyMenuListener;
import me.muszek_.playerBounty.listeners.UpdateNotifyListener;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.tasks.ExpireBountiesTask;
import me.muszek_.playerBounty.utils.UpdateChecker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class PlayerBounty extends JavaPlugin {

	private static PlayerBounty instance;

	private String latestVersion;
	private boolean updateAvailable = false;

	@Override
	public void onEnable() {
		instance = this;

		YamlUpdater updater = new YamlUpdater(this);
		FileConfiguration config = updater.update("config.yml");

		setupLanguages(updater, config);

		long periodSeconds = config.getLong("expire-check-interval-seconds", 3600L);
		new ExpireBountiesTask(this).runTaskTimer(this, 20L, periodSeconds * 20L);

		CommandManager commandManager = new CommandManager(this);
		var pluginCmd = getCommand("playerbounty");
		if (pluginCmd != null) {
			pluginCmd.setExecutor(commandManager);
			pluginCmd.setTabCompleter(commandManager);
		}

		new BountyMenuListener(this);

		getServer().getPluginManager().registerEvents(new BountyCompleteListener(this), this);
		getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);

		int pluginId = 26963;
		try {
			new Metrics(this, pluginId);
		} catch (Exception e) {
			getLogger().warning("Nie udało się uruchomić Metrics (bStats).");
		}

		int spigotResourceId = 128132;

		new UpdateChecker(this, spigotResourceId).getLatestVersion(version -> {

			String current = this.getDescription().getVersion();
			this.latestVersion = version;
			this.updateAvailable = !current.equalsIgnoreCase(version);

			if (!updateAvailable) {
				getLogger().info("Plugin PlayerBounty is up to date.");
			} else {
				getLogger().warning("Plugin PlayerBounty has an update!");
				getLogger().warning("Download: https://www.spigotmc.org/resources/" + spigotResourceId + "/");
			}
		});
	}

	@Override
	public void onDisable() {
		getLogger().info("Player Bounty plugin has been disabled!");
	}

	public static PlayerBounty getInstance() {
		return instance;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable;
	}

	public String getLatestVersion() {
		return latestVersion;
	}

	private void setupLanguages(YamlUpdater updater, FileConfiguration config) {
		File langsDir = new File(getDataFolder(), "langs");
		if (!langsDir.exists()) {
			langsDir.mkdirs();
		}

		if (!new File(langsDir, "en.yml").exists()) saveResource("langs/en.yml", false);
		if (!new File(langsDir, "pl.yml").exists()) saveResource("langs/pl.yml", false);

		FileConfiguration langEn = updater.update("langs/en.yml");
		FileConfiguration langPl = updater.update("langs/pl.yml");

		String locale = config.getString("settings.locale", "pl").toLowerCase();
		FileConfiguration langConfig = locale.equals("en") ? langEn : langPl;

		Settings.load(langConfig);
	}
}