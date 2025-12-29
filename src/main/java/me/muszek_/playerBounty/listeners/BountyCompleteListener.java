package me.muszek_.playerBounty.listeners;

import java.util.Objects;
import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

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

        for (String key : Objects.requireNonNull(cfg.getConfigurationSection("bounties")).getKeys(false)) {
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
                    assert createdStr != null;
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
        List<String> itemRewardsNames = new ArrayList<>();

        for (String id : toProcess) {
            String path = "bounties." + id;
            if (cfg.contains(path + ".reward-item")) {
                ItemStack item = cfg.getItemStack(path + ".reward-item");
                if (item != null) {
                    killer.getInventory().addItem(item).forEach((idx, left) ->
                        killer.getWorld().dropItem(killer.getLocation(), left));

                    String itemName = item.getType().name();
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        itemName = item.getItemMeta().getDisplayName();
                    }
                    itemRewardsNames.add(itemName + " x" + item.getAmount());
                }
            } else {
                totalReward += cfg.getDouble(path + ".amount", 0);
            }
            cfg.set(path, null);
        }

        try {
            cfg.save(bountyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save bounties.yml: " + e.getMessage());
        }

        if (totalReward > 0) {
            double finalTotalReward = totalReward;
            Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getServer().dispatchCommand(
                    Bukkit.getConsoleSender(),
                    String.format("eco give %s %s", killer.getName(), finalTotalReward)
                )
            );
        }

        String idPart = stacked
            ? String.join(",", toProcess)
            : toProcess.get(0);

        String rewardString;
        if (totalReward > 0 && !itemRewardsNames.isEmpty()) {
            rewardString = totalReward + " + " + String.join(", ", itemRewardsNames);
        } else if (totalReward > 0) {
            rewardString = String.valueOf(totalReward);
        } else {
            rewardString = String.join(", ", itemRewardsNames);
        }

        Bukkit.broadcast(Colors.color(
            Settings.LangKey.BOUNTY_COMPLETE.get(),
            "%id%", idPart,
            "%player%", victim.getName(),
            "%killer%", killer.getName(),
            "%amount%", rewardString
        ));
    }
}