package me.muszek_.playerBounty.settings;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.YamlUpdater;
import org.bukkit.configuration.file.FileConfiguration;

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
		BOUNTY_REFUNDED("Bounty.Refunded"),
		BOUNTY_COMPLETE("Bounty.Complete"),
		BOUNTY_CREATED("Bounty.Created"),
		BOUNTY_CREATE_USAGE("Bounty.Create_Usage"),
		BOUNTY_NOT_ENOUGH_MONEY("Bounty.Not_Enough_Money"),
		BOUNTY_SAVE_ERROR("Bounty.Save_Error"),
		BOUNTY_BROADCAST("Bounty.Create_Broadcast"),
		BOUNTY_NOT_FOUND("Bounty.Not_Found"),
		BOUNTY_REMOVED("Bounty.Removed"),
		BOUNTY_REMOVE_BROADCAST("Bounty.Remove_Broadcast"),
		BOUNTY_REMOVE_USAGE("Bounty.Remove_Usage"),
		BOUNTY_EXPIRED("Bounty.Expired"),
		BOUNTY_CREATED_FULL("Bounty.Created_Full"),
		BOUNTY_ITEM_REQUIRED("Bounty.Item_Required"),
		BOUNTY_ITEM_REWARD_RECEIVED("Bounty.Item_Reward_Received"),
		BOUNTY_LIMIT_REACHED("Bounty.Limit_Reached");

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
						List<String> lines = ((List<Object>) raw).stream()
								.map(Object::toString)
								.map(Colors::color)
								.toList();
						key.value = String.join("\n", lines);
					} else {
						key.value = Colors.color(langConfig.getString(key.path, "§cMissing lang: " + key.path));
					}
				} else {
					key.value = "§cMissing lang: " + key.path;
				}
			}
		}
	}

	public enum ConfigKey {
		BOUNTY_TIME("Bounty.Time", 3),
		BOUNTY_STACKED("Bounty.Stacked", false),
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

	/**
	 * Load default config.yml and default Polish language
	 */
	public static void load() {
		PlayerBounty plugin = PlayerBounty.getInstance();
		YamlUpdater updater = new YamlUpdater(plugin);
		FileConfiguration config = updater.update("config.yml");
		FileConfiguration lang = updater.update("langs/pl.yml");

		LangKey.load(lang);
		ConfigKey.load(config);
	}

	/**
	 * Load config.yml (merged defaults) and specified language
	 *
	 * @param langConfig pre-loaded language config
	 */
	public static void load(FileConfiguration langConfig) {
		PlayerBounty plugin = PlayerBounty.getInstance();
		YamlUpdater updater = new YamlUpdater(plugin);
		FileConfiguration config = updater.update("config.yml");

		LangKey.load(langConfig);
		ConfigKey.load(config);
	}
}
