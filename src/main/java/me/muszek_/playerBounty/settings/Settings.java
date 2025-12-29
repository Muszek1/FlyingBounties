package me.muszek_.playerBounty.settings;

import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.YamlUpdater;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.List;

public final class Settings {

	public enum LangKey {
		PLAYER_NOT_FOUND("Player_Not_Found"),
		MOB_NOT_FOUND("Mob_Not_Found"),
		NO_PERMISSION("No_Permissions"),
		PLUGIN_RELOADED("Plugin_Reloaded"),
		WRONG_NUMBER("Wrong_Number"),
		GUI_USAGE("Gui.Usage"),
		HELP("Help"),

		BOUNTY_COMPLETE("Bounty.Complete"),
		BOUNTY_CREATED("Bounty.Created"),
		BOUNTY_CREATED_ITEM("Bounty.Created_Item"),
		BOUNTY_CREATE_USAGE("Bounty.Create_Usage"),
		BOUNTY_NOT_ENOUGH_MONEY("Bounty.Not_Enough_Money"),
		BOUNTY_SAVE_ERROR("Bounty.Save_Error"),
		BOUNTY_BROADCAST("Bounty.Create_Broadcast"),
		BOUNTY_BROADCAST_ITEM("Bounty.Create_Broadcast_Item"),
		BOUNTY_NOT_FOUND("Bounty.Not_Found"),
		BOUNTY_REMOVED("Bounty.Removed"),
		BOUNTY_REMOVE_BROADCAST("Bounty.Remove_Broadcast"),
		BOUNTY_REMOVE_USAGE("Bounty.Remove_Usage"),
		BOUNTY_EXPIRED("Bounty.Expired"),
		BOUNTY_LIMIT_REACHED("Bounty.Limit_Reached"),
		BOUNTY_REFUNDED("Bounty.Refunded"),
		BOUNTY_SELF_NOT_ALLOWED("Bounty.Self_Not_Allowed");

		private final String path;
		private String value;

		LangKey(String path) {
			this.path = path;
		}

		public String get() {
			return value;
		}

		static void load(FileConfiguration langConfig) {
			for (LangKey key : values()) {
				if (langConfig.contains(key.path)) {
					Object raw = langConfig.get(key.path);
					if (raw instanceof List<?>) {
						@SuppressWarnings("unchecked")
						List<String> lines = (List<String>) raw;
						key.value = String.join("\n", lines);
					} else {
						key.value = langConfig.getString(key.path, "&cBrak tłumaczenia: " + key.path);
					}
				} else {
					key.value = "&cBrak tłumaczenia: " + key.path;
				}
			}
		}
	}

	public enum ConfigKey {
		BOUNTY_TIME("Bounty.Time", 1440),
		BOUNTY_STACKED("Bounty.Stacked", true),
		BOUNTY_LIMIT("Bounty.Limit", 3);

		private final String path;
		private final Object defaultValue;
		private Object value;

		ConfigKey(String path, Object defaultValue) {
			this.path = path;
			this.defaultValue = defaultValue;
		}

		@SuppressWarnings("unchecked")
		public <T> T get() {
			return (T) value;
		}

		static void load(FileConfiguration config) {
			for (ConfigKey key : values()) {
				if (key.defaultValue instanceof Integer) {
					key.value = config.getInt(key.path, (Integer) key.defaultValue);
				} else if (key.defaultValue instanceof Boolean) {
					key.value = config.getBoolean(key.path, (Boolean) key.defaultValue);
				} else {
					key.value = config.getString(key.path, String.valueOf(key.defaultValue));
				}
			}
		}
	}

	public static void load() {
		PlayerBounty plugin = PlayerBounty.getInstance();
		plugin.reloadConfig();

		YamlUpdater updater = new YamlUpdater(plugin);

		FileConfiguration config = updater.update("config.yml");

		updater.update("langs/en.yml");
		updater.update("langs/pl.yml");

		String locale = config.getString("settings.locale", "en").toLowerCase();

		FileConfiguration langConfig;
		if (locale.equals("pl")) {
			langConfig = updater.update("langs/pl.yml");
		} else {
			langConfig = updater.update("langs/en.yml");
		}

		LangKey.load(langConfig);
		ConfigKey.load(config);
	}

	public static void load(FileConfiguration langConfig) {
		PlayerBounty plugin = PlayerBounty.getInstance();
		YamlUpdater updater = new YamlUpdater(plugin);
		FileConfiguration config = updater.update("config.yml");

		LangKey.load(langConfig);
		ConfigKey.load(config);
	}
}