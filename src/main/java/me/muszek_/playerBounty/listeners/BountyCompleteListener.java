package me.muszek_.playerBounty.listeners;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BountyCompleteListener implements Listener {
    private final PlayerBounty plugin;
    private final File bountyFile;

    public BountyCompleteListener(PlayerBounty plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.bountyFile = new File(dataFolder, "bounties.yml");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        boolean stacked = Settings.ConfigKey.BOUNTY_STACKED.<Boolean>get();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);
        if (!cfg.contains("bounties")) return;

        UUID victimId = victim.getUniqueId();
        List<String> matches = new ArrayList<>();

        for (String key : cfg.getConfigurationSection("bounties").getKeys(false)) {
            String basePath = "bounties." + key;
            String uuidString = cfg.getString(basePath + ".target-uuid");
            if (uuidString == null) continue;

            UUID storedId;
            try {
                storedId = UUID.fromString(uuidString);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            if (victimId.equals(storedId)) {
                matches.add(key);
            }
        }

        if (matches.isEmpty()) return;

        List<String> toProcess = new ArrayList<>();
        if (stacked) {
            toProcess.addAll(matches);
        } else {
            String oldestKey = null;
            Instant oldestTime = Instant.MAX;
            for (String key : matches) {
                String createdStr = cfg.getString("bounties." + key + ".created");
                try {
                    Instant created = Instant.parse(createdStr);
                    if (created.isBefore(oldestTime)) {
                        oldestTime = created;
                        oldestKey = key;
                    }
                } catch (Exception ignored) {
                }
            }
            if (oldestKey != null) {
                toProcess.add(oldestKey);
            }
        }

        if (toProcess.isEmpty()) return;

        double totalReward = 0;

        for (String id : toProcess) {
            String base = "bounties." + id;
            totalReward += cfg.getDouble(base + ".amount", 0);

            if (cfg.contains(base + ".item-reward")) {
                try {
                    org.bukkit.inventory.ItemStack item = cfg.getItemStack(base + ".item-reward");
                    if (item != null && !item.getType().isAir()) {
                        killer.getInventory().addItem(item);
                        killer.sendMessage(Colors.color(
                                Settings.LangKey.BOUNTY_ITEM_REWARD_RECEIVED.get()
                                        .replace("%item%", item.getType().name())
                        ));

                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error! Nie udało się odczytać item-reward dla bounty #" + id);
                }
            }

            cfg.set("bounties." + id, null);
        }


        try {
            cfg.save(bountyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save bounties.yml: " + e.getMessage());
        }

        double finalTotalReward = totalReward;
        Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getServer().dispatchCommand(
                        Bukkit.getConsoleSender(),
                        String.format("eco give %s %s", killer.getName(), finalTotalReward)
                )
        );

        String idPart = stacked
                ? String.join(",", toProcess)
                : toProcess.get(0);
        String template = Settings.LangKey.BOUNTY_COMPLETE.get();
        String msg = template
                .replace("%id%", idPart)
                .replace("%player%", victim.getName())
                .replace("%killer%", killer.getName())
                .replace("%amount%", String.valueOf(totalReward));
        Bukkit.broadcastMessage(Colors.color(msg));
    }
}
