package me.muszek_.playerBounty.tasks;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.settings.Settings;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class ExpireBountiesTask extends BukkitRunnable {
    private final PlayerBounty plugin;
    private final File bountyFile;

    public ExpireBountiesTask(PlayerBounty plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.bountyFile = new File(dataFolder, "bounties.yml");
    }

    @Override
    public void run() {
        if (!bountyFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);
        ConfigurationSection section = cfg.getConfigurationSection("bounties");
        if (section == null) return;

        Instant now = Instant.now();
        List<String> expiredIds = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            String expiresStr = cfg.getString("bounties." + key + ".expires");
            if (expiresStr == null) continue;
            try {
                Instant expires = Instant.parse(expiresStr);
                if (now.isAfter(expires)) expiredIds.add(key);
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid expires format for bounty #" + key + " (" + ex.getMessage() + ")");
            }
        }

        if (expiredIds.isEmpty()) return;

        Map<String, String> idToTarget = new HashMap<>();
        Map<UUID, Double> refundsByIssuerUuid = new HashMap<>();
        Map<String, Double> refundsByIssuerName = new HashMap<>();

        for (String id : expiredIds) {
            String base = "bounties." + id;
            double amount = cfg.getDouble(base + ".amount", 0.0);
            String issuerUuidStr = cfg.getString(base + ".issuer-uuid", null);
            String issuerName = cfg.getString(base + ".issuer", null);
            String targetName = cfg.getString(base + ".target-name",
                    cfg.getString(base + ".target", "unknown"));

            idToTarget.put(id, targetName == null ? "unknown" : targetName);

            if (issuerUuidStr != null) {
                try {
                    UUID u = UUID.fromString(issuerUuidStr);
                    refundsByIssuerUuid.put(u, refundsByIssuerUuid.getOrDefault(u, 0.0) + amount);
                } catch (Exception ignored) {
                    if (issuerName != null) refundsByIssuerName.put(issuerName, refundsByIssuerName.getOrDefault(issuerName, 0.0) + amount);
                }
            } else if (issuerName != null) {
                refundsByIssuerName.put(issuerName, refundsByIssuerName.getOrDefault(issuerName, 0.0) + amount);
            } else {
                plugin.getLogger().warning("Bounty #" + id + " has no issuer recorded, skipping refund.");
            }
        }

        for (String id : expiredIds) {
            cfg.set("bounties." + id, null);
        }
        try {
            cfg.save(bountyFile);
        } catch (IOException io) {
            plugin.getLogger().severe("Failed to save bounties.yml after expiring bounties: " + io.getMessage());
        }

        String expiredTemplate = Settings.LangKey.BOUNTY_EXPIRED.get();
        for (String id : expiredIds) {
            String target = idToTarget.getOrDefault(id, "unknown");
            String msg = expiredTemplate
                    .replace("%id%", id)
                    .replace("%player%", target);
            Bukkit.broadcastMessage(Colors.color(msg));
        }

        Economy economy = null;
        var reg = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (reg != null) economy = reg.getProvider();

        List<String> refundErrors = new ArrayList<>();

        for (Map.Entry<UUID, Double> e : refundsByIssuerUuid.entrySet()) {
            UUID uuid = e.getKey();
            double amount = e.getValue();
            try {
                if (economy != null) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                    EconomyResponse r = economy.depositPlayer(off, amount);
                    if (r == null || !r.transactionSuccess()) {
                        refundErrors.add("Failed deposit to " + uuid + " : " + (r == null ? "null response" : r.errorMessage));
                    }
                } else {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
                    String name = off.getName();
                    if (name != null) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("eco give %s %s", name, amount));
                    } else {
                        refundErrors.add("No name for uuid " + uuid + ", cannot fallback deposit");
                    }
                }
            } catch (Exception ex) {
                refundErrors.add("Exception refund to uuid " + uuid + " : " + ex.getMessage());
            }
        }

        for (Map.Entry<String, Double> e : refundsByIssuerName.entrySet()) {
            String name = e.getKey();
            double amount = e.getValue();
            try {
                if (economy != null) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(name);
                    EconomyResponse r = economy.depositPlayer(off, amount);
                    if (r == null || !r.transactionSuccess()) {
                        refundErrors.add("Failed deposit to " + name + " : " + (r == null ? "null response" : r.errorMessage));
                    }
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("eco give %s %s", name, amount));
                }
            } catch (Exception ex) {
                refundErrors.add("Exception refund to " + name + " : " + ex.getMessage());
            }
        }

        String refundedTemplate = Settings.LangKey.BOUNTY_REFUNDED.get();
        for (Map.Entry<String, Double> e : refundsByIssuerName.entrySet()) {
            String msg = refundedTemplate
                    .replace("%issuer%", e.getKey())
                    .replace("%amount%", String.valueOf(e.getValue()));
            Bukkit.broadcastMessage(Colors.color(msg));
        }
        for (Map.Entry<UUID, Double> e : refundsByIssuerUuid.entrySet()) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(e.getKey());
            String name = off.getName() != null ? off.getName() : e.getKey().toString();
            String msg = refundedTemplate
                    .replace("%issuer%", name)
                    .replace("%amount%", String.valueOf(e.getValue()));
            Bukkit.broadcastMessage(Colors.color(msg));
        }

        if (!refundErrors.isEmpty()) {
            plugin.getLogger().warning("Some refunds failed during ExpireBountiesTask:");
            refundErrors.forEach(s -> plugin.getLogger().warning("  - " + s));
        }
    }
}
