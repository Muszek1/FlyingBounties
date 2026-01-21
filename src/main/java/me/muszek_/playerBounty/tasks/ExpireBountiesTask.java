package me.muszek_.playerBounty.tasks;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.storage.Bounty;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ExpireBountiesTask extends BukkitRunnable {

  private final PlayerBounty plugin;

  public ExpireBountiesTask(PlayerBounty plugin) {
    this.plugin = plugin;
  }

  @Override
  public void run() {
    plugin.getBountyStorage().getExpiredBounties(Instant.now().toEpochMilli())
        .thenAccept(expiredBounties -> {
          if (expiredBounties.isEmpty()) {
            return;
          }

          Bukkit.getScheduler().runTask(plugin, () -> {
            Map<String, Double> refundsByIssuerName = new HashMap<>();

            for (Bounty bounty : expiredBounties) {
              if (bounty.isItemReward()) {
                ItemStack item = bounty.getRewardItem();
                String issuerName = bounty.getIssuer();
                if (item != null && issuerName != null) {
                  Player issuer = Bukkit.getPlayerExact(issuerName);
                  if (issuer != null && issuer.isOnline()) {
                    issuer.getInventory().addItem(item).forEach((idx, left) ->
                        issuer.getWorld().dropItem(issuer.getLocation(), left));
                    issuer.sendMessage(Colors.color("&eZlecenie #" + bounty.getId()
                        + " wygasło. Przedmiot wrócił do twojego ekwipunku."));
                  }
                }
              } else {
                String issuerName = bounty.getIssuer();
                if (issuerName != null) {
                  refundsByIssuerName.put(issuerName,
                      refundsByIssuerName.getOrDefault(issuerName, 0.0) + bounty.getAmount());
                }
              }
              plugin.getBountyStorage().deleteBounty(bounty.getId());
            }

            processMoneyRefunds(refundsByIssuerName);
          });
        });
  }

  private void processMoneyRefunds(Map<String, Double> refunds) {
    if (refunds.isEmpty()) {
      return;
    }

    Economy economy = null;
    var reg = Bukkit.getServicesManager().getRegistration(Economy.class);
    if (reg != null) {
      economy = reg.getProvider();
    }

    String template = Settings.LangKey.BOUNTY_EXPIRED.get();

    for (Map.Entry<String, Double> e : refunds.entrySet()) {
      String name = e.getKey();
      double amount = e.getValue();

      OfflinePlayer op = Bukkit.getOfflinePlayer(name);

      if (economy != null) {
        economy.depositPlayer(op, amount);
      } else {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
            String.format("eco give %s %s", name, amount));
      }

      Bukkit.broadcast(Colors.color(template,
          "%issuer%", name,
          "%amount%", String.valueOf(amount)
      ));
    }
  }

  private void processRefund(Economy economy, OfflinePlayer player, double amount,
      String identifier, List<String> errors) {
    try {
      if (economy != null) {
        EconomyResponse r = economy.depositPlayer(player, amount);
        if (r == null || !r.transactionSuccess()) {
          errors.add("Failed deposit to " + identifier + " : " + (r == null ? "null response"
              : r.errorMessage));
        }
      } else {
        String name = player.getName();
        if (name != null) {
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
              String.format("eco give %s %s", name, amount));
        } else {
          errors.add("No name for " + identifier + ", cannot fallback deposit");
        }
      }
    } catch (Exception ex) {
      errors.add("Exception refund to " + identifier + " : " + ex.getMessage());
    }
  }
}