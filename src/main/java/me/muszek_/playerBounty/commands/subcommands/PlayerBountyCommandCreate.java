package me.muszek_.playerBounty.commands.subcommands;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.utils.TabCompletePlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class PlayerBountyCommandCreate extends SubCommand {

  @Override
  public String getName() {
    return "create";
  }

  @Override
  public String getDescription() {
    return "Creates a bounty on a player";
  }

  @Override
  public String getSyntax() {
    return "/bounty create <player> <amount|item>";
  }

  @Override
  public String getPermission() {
    return "flyingbounties.create";
  }

  @Override
  public void perform(Player player, String[] args) {
    if (args.length < 3) {
      player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_CREATE_USAGE.get()));
      return;
    }

    String targetName = args[1];

    CompletableFuture.supplyAsync(() -> Bukkit.getOfflinePlayer(targetName))
        .thenAccept(target -> {
          Bukkit.getScheduler().runTask(PlayerBounty.getInstance(), () -> {
            processBountyCreation(player, target, args);
          });
        });
  }

  private void processBountyCreation(Player player, OfflinePlayer target, String[] args) {
    if (!target.hasPlayedBefore() && !target.isOnline()) {
      player.sendMessage(
          Colors.color(Settings.LangKey.PLAYER_NOT_FOUND.get(), "%player%", args[1]));
      return;
    }

    if (player.getUniqueId().equals(target.getUniqueId())) {
      player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_SELF_NOT_ALLOWED.get(), "%player%",
          player.getName()));
      return;
    }

    double amountVal = 0;
    ItemStack itemRewardVal = null;
    boolean isItemVal = args[2].equalsIgnoreCase("item");

    if (isItemVal) {
      itemRewardVal = player.getInventory().getItemInMainHand();
      if (itemRewardVal == null || itemRewardVal.getType() == Material.AIR) {
        player.sendMessage(Colors.color("&cMusisz trzymać przedmiot w ręce!"));
        return;
      }
      itemRewardVal = itemRewardVal.clone();
    } else {
      try {
        amountVal = Double.parseDouble(args[2]);
        if (amountVal <= 0) {
          throw new NumberFormatException();
        }
      } catch (NumberFormatException ex) {
        player.sendMessage(Colors.color(Settings.LangKey.WRONG_NUMBER.get()));
        return;
      }

      Economy economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
      if (!economy.has(player, amountVal)) {
        player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_NOT_ENOUGH_MONEY.get()));
        return;
      }
    }

    final double amount = amountVal;
    final ItemStack itemReward = itemRewardVal;
    final boolean isItem = isItemVal;

    PlayerBounty plugin = PlayerBounty.getInstance();

    plugin.getBountyStorage().getBountiesByIssuer(player.getName()).thenAccept(playerBounties -> {
      Bukkit.getScheduler().runTask(plugin, () -> {
        if (!checkLimit(player, plugin, playerBounties.size())) {
          return;
        }

        Economy economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();

        if (isItem) {
          ItemStack currentItem = player.getInventory().getItemInMainHand();
          if (currentItem == null || !currentItem.isSimilar(itemReward)
              || currentItem.getAmount() < itemReward.getAmount()) {
            player.sendMessage(Colors.color(
                "&cPrzedmiot w twojej ręce uległ zmianie! Anulowano wystawianie zlecenia."));
            return;
          }
          player.getInventory().setItemInMainHand(null);
        } else {
          if (!economy.has(player, amount)) {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_NOT_ENOUGH_MONEY.get()));
            return;
          }
          economy.withdrawPlayer(player, amount);
        }

        long durationSeconds = plugin.getConfig().getInt("bounty-time",
            Settings.ConfigKey.BOUNTY_TIME.get()) * 60L;

        plugin.getBountyStorage().createBounty(
            player,
            target.getName(),
            target.getUniqueId(),
            amount,
            itemReward,
            durationSeconds
        ).thenAccept(bounty -> {
          Bukkit.getScheduler().runTask(plugin, () -> {
            if (bounty == null) {
              player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_SAVE_ERROR.get()));
              if (!isItem) {
                economy.depositPlayer(player, amount);
              } else {
                player.getInventory().addItem(itemReward);
              }
              return;
            }

            String displayPrice;
            if (isItem) {
              displayPrice = itemReward.getType().name();
              if (itemReward.hasItemMeta() && itemReward.getItemMeta().hasDisplayName()) {
                displayPrice = itemReward.getItemMeta().getDisplayName();
              } else {
                displayPrice = displayPrice + " x" + itemReward.getAmount();
              }

              player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_CREATED_ITEM.get(),
                  "%player%", target.getName(),
                  "%item%", displayPrice
              ));

              Bukkit.broadcast(Colors.color(Settings.LangKey.BOUNTY_BROADCAST_ITEM.get(),
                  "%id%", String.valueOf(bounty.getId()),
                  "%player%", target.getName(),
                  "%item%", displayPrice,
                  "%issuer%", player.getName()
              ));
            } else {
              displayPrice = String.valueOf(amount);
              player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_CREATED.get(),
                  "%player%", target.getName(),
                  "%price%", displayPrice
              ));

              Bukkit.broadcast(Colors.color(Settings.LangKey.BOUNTY_BROADCAST.get(),
                  "%id%", String.valueOf(bounty.getId()),
                  "%player%", target.getName(),
                  "%price%", displayPrice,
                  "%issuer%", player.getName()
              ));
            }
          });
        });
      });
    });
  }

  private boolean checkLimit(Player player, PlayerBounty plugin, int activeCount) {
    if (player.hasPermission("flyingbounties.limit.unlimited")) {
      return true;
    }

    int limit = plugin.getConfig()
        .getInt("bounty-default-limit", (int) Settings.ConfigKey.BOUNTY_LIMIT.get());

    for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
      String perm = pai.getPermission();
      if (perm.startsWith("flyingbounties.limit.")) {
        try {
          int customLimit = Integer.parseInt(perm.substring("flyingbounties.limit.".length()));
          limit = Math.max(limit, customLimit);
        } catch (NumberFormatException ignored) {
        }
      }
    }

    if (activeCount >= limit) {
      player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_LIMIT_REACHED.get(), "%limit%",
          String.valueOf(limit)));
      return false;
    }
    return true;
  }

  @Override
  public List<String> getSubcommandArguments(Player player, String[] args) {
    if (args.length == 2) {
      return TabCompletePlayer.getOnlinePlayerNames();
    }
    return null;
  }
}