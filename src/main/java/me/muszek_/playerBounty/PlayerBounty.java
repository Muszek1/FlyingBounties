package me.muszek_.playerBounty;

import me.muszek_.playerBounty.commands.CommandManager;
import me.muszek_.playerBounty.listeners.BountyCompleteListener;
import me.muszek_.playerBounty.listeners.BountyMenuListener;
import me.muszek_.playerBounty.listeners.UpdateNotifyListener;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.tasks.ExpireBountiesTask;
import me.muszek_.playerBounty.utils.Logger;
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

		long periodSeconds = getConfig().getLong("expire-check-interval-seconds", 3600L);
		new ExpireBountiesTask(this)
				.runTaskTimer(this, 0L, periodSeconds * 20L);


		CommandManager commandManager = new CommandManager(this);
		new BountyMenuListener(this);

		getCommand("playerbounty").setExecutor(commandManager);
		getCommand("playerbounty").setTabCompleter(commandManager);
		getServer().getPluginManager().registerEvents(new BountyCompleteListener(this), this);
		getServer().getPluginManager().registerEvents(new BountyMenuListener(this), this);
		getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);


		int pluginId = 26963;
		Metrics metrics = new Metrics(this, pluginId);


	}


	public boolean isUpdateAvailable() {
		return updateAvailable;
	}

	public String getLatestVersion() {
		return latestVersion;
	}

	@Override
	public void onDisable() {
		getLogger().warning("Player Bounty plugin has been disabled!");

	}

	public static PlayerBounty getInstance() {
		return instance;
	}

}
