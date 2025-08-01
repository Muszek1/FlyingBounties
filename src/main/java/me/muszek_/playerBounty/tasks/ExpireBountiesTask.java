package me.muszek_.playerBounty.tasks;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ExpireBountiesTask extends BukkitRunnable {
    private final PlayerBounty plugin;
    private final File bountyFile;
    private final FileConfiguration config;

    public ExpireBountiesTask(PlayerBounty plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.bountyFile = new File(dataFolder, "bounties.yml");
        this.config = YamlConfiguration.loadConfiguration(bountyFile);
    }

    @Override
    public void run() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);
        ConfigurationSection section = cfg.getConfigurationSection("bounties");
        if (section == null) return;

        Instant now = Instant.now();
        List<String> toRemove = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            String path = "bounties." + key;
            String expiresStr = cfg.getString(path + ".expires");
            if (expiresStr == null) continue;
            Instant expires;
            try {
                expires = Instant.parse(expiresStr);
            } catch (Exception ex) {
                plugin.getLogger().warning("Nieprawidłowy format daty wygaśnięcia dla zlecenia #" + key);
                continue;
            }
            if (now.isAfter(expires)) {
                toRemove.add(key);
            }
        }

        if (toRemove.isEmpty()) return;

        for (String key : toRemove) {
            String path = "bounties." + key;
            String target = cfg.getString(path + ".target", "nieznany");
            cfg.set(path, null);
            String template = Settings.LangKey.BOUNTY_EXPIRED.get();
            String msg = template
                    .replace("%id%", key)
                    .replace("%player%", target);
            Bukkit.broadcastMessage(Colors.color(msg));
        }

        try {
            cfg.save(bountyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie udało się zapisać pliku bounties.yml po usunięciu przeterminowanych zleceń");
            e.printStackTrace();
        }
    }
}
