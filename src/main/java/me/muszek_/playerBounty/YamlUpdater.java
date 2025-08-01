package me.muszek_.playerBounty;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class YamlUpdater {

	private final JavaPlugin plugin;

	public YamlUpdater(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public FileConfiguration update(String fileName) {
		File file = new File(plugin.getDataFolder(), fileName);
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}

		if (!file.exists()) {
			plugin.saveResource(fileName, false);
		}

		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		InputStream defaultStream = plugin.getResource(fileName);

		if (defaultStream != null) {
			YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
					new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
			);
			boolean changed = false;

			Set<String> keys = defaultConfig.getKeys(true);
			for (String key : keys) {
				if (!config.contains(key)) {
					config.set(key, defaultConfig.get(key));
					changed = true;
				}
			}

			if (changed) {
				try {
					config.save(file);
					plugin.getLogger().info("Updated missing lines in " + fileName);
				} catch (IOException e) {
					plugin.getLogger().severe("ERROR! Cannot save " + fileName + ": " + e.getMessage());
				}
			}
		}

		return config;
	}
}
