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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
                if (now.isAfter(expires)) {
                    if (cfg.contains("bounties." + key + ".reward-item")) {
                        String issuerName = cfg.getString("bounties." + key + ".issuer");
                        Player issuer = issuerName != null ? Bukkit.getPlayerExact(issuerName) : null;
                        if (issuer == null || !issuer.isOnline()) {
                            continue;
                        }
                    }
                    expiredIds.add(key);
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid expires format for bounty #" + key + " (" + ex.getMessage() + ")");
            }
        }

        if (expiredIds.isEmpty()) return;

        Map<String, Double> refundsByIssuerName = new HashMap<>();
        Map<UUID, Double> refundsByIssuerUuid = new HashMap<>();

        for (String id : expiredIds) {
            String base = "bounties." + id;

            if (cfg.contains(base + ".reward-item")) {
                ItemStack item = cfg.getItemStack(base + ".reward-item");
                String issuerName = cfg.getString(base + ".issuer");
                if (item != null && issuerName != null) {
                    Player issuer = Bukkit.getPlayerExact(issuerName);
                    if (issuer != null && issuer.isOnline()) {
                        issuer.getInventory().addItem(item).forEach((idx, left) ->
                            issuer.getWorld().dropItem(issuer.getLocation(), left));
                        issuer.sendMessage(Colors.color("&eZlecenie #" + id + " wygasło. Przedmiot wrócił do twojego ekwipunku."));
                    }
                }
                continue;
            }

            double amount = cfg.getDouble(base + ".amount", 0.0);
            String issuerUuidStr = cfg.getString(base + ".issuer-uuid", null);
            String issuerName = cfg.getString(base + ".issuer", null);

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

        Economy economy = null;
        var reg = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (reg != null) economy = reg.getProvider();

        List<String> refundErrors = new ArrayList<>();

        for (Map.Entry<UUID, Double> e : refundsByIssuerUuid.entrySet()) {
            UUID uuid = e.getKey();
            double amount = e.getValue();
            processRefund(economy, Bukkit.getOfflinePlayer(uuid), amount, uuid.toString(), refundErrors);
        }

        for (Map.Entry<String, Double> e : refundsByIssuerName.entrySet()) {
            String name = e.getKey();
            double amount = e.getValue();
            processRefund(economy, Bukkit.getOfflinePlayer(name), amount, name, refundErrors);
        }

        for (String id : expiredIds) {
            cfg.set("bounties." + id, null);
        }

        try {
            cfg.save(bountyFile);
        } catch (IOException io) {
            plugin.getLogger().severe("Failed to save bounties.yml after expiring bounties: " + io.getMessage());
        }

        String template = Settings.LangKey.BOUNTY_EXPIRED.get();

        for (Map.Entry<String, Double> e : refundsByIssuerName.entrySet()) {
            Bukkit.broadcast(Colors.color(template,
                "%issuer%", e.getKey(),
                "%amount%", String.valueOf(e.getValue())
            ));
        }

        for (Map.Entry<UUID, Double> e : refundsByIssuerUuid.entrySet()) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(e.getKey());
            String name = off.getName() != null ? off.getName() : "Nieznany";

            Bukkit.broadcast(Colors.color(template,
                "%issuer%", name,
                "%amount%", String.valueOf(e.getValue())
            ));
        }

        if (!refundErrors.isEmpty()) {
            plugin.getLogger().warning("Some refunds failed during ExpireBountiesTask:");
            refundErrors.forEach(s -> plugin.getLogger().warning("  - " + s));
        }
    }

    private void processRefund(Economy economy, OfflinePlayer player, double amount, String identifier, List<String> errors) {
        try {
            if (economy != null) {
                EconomyResponse r = economy.depositPlayer(player, amount);
                if (r == null || !r.transactionSuccess()) {
                    errors.add("Failed deposit to " + identifier + " : " + (r == null ? "null response" : r.errorMessage));
                }
            } else {
                String name = player.getName();
                if (name != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("eco give %s %s", name, amount));
                } else {
                    errors.add("No name for " + identifier + ", cannot fallback deposit");
                }
            }
        } catch (Exception ex) {
            errors.add("Exception refund to " + identifier + " : " + ex.getMessage());
        }
    }
}