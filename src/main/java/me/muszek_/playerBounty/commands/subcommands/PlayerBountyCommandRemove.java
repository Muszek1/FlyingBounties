package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.storage.Bounty;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerBountyCommandRemove extends SubCommand {

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return "Removes a bounty (issuer or admin)";
    }

    @Override
    public String getSyntax() {
        return "/bounty remove <id>";
    }

    @Override
    public String getPermission() {
        return "flyingbounties.remove";
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_REMOVE_USAGE.get()));
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(Colors.color(Settings.LangKey.WRONG_NUMBER.get()));
            return;
        }

        PlayerBounty plugin = PlayerBounty.getInstance();
        plugin.getBountyStorage().getBounty(id).thenAccept(bountyOpt -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (bountyOpt.isEmpty()) {
                    player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_NOT_FOUND.get(), "%id%", String.valueOf(id)));
                    return;
                }

                Bounty bounty = bountyOpt.get();
                String issuer = bounty.getIssuer();
                boolean isAdmin = player.hasPermission("flyingbounties.admin");

                if (!isAdmin && (issuer == null || !issuer.equals(player.getName()))) {
                    player.sendMessage(Colors.color(Settings.LangKey.NO_PERMISSION.get()));
                    return;
                }

                if (bounty.isItemReward()) {
                    ItemStack item = bounty.getRewardItem();
                    if (item != null) {
                        Player issuerPlayer = issuer != null ? Bukkit.getPlayerExact(issuer) : null;
                        if (issuerPlayer != null && issuerPlayer.isOnline()) {
                            issuerPlayer.getInventory().addItem(item).forEach((idx, left) ->
                                issuerPlayer.getWorld().dropItem(issuerPlayer.getLocation(), left));

                            String itemName = item.getType().name();
                            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                                itemName = item.getItemMeta().getDisplayName();
                            }
                            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_REFUNDED.get(),
                                "%amount%", itemName,
                                "%issuer%", issuer
                            ));
                        } else if (!isAdmin) {
                            player.sendMessage(Colors.color("&cMusisz być online, aby odebrać zwrot przedmiotu, lub przedmiot przepadnie!"));
                        }
                    }
                } else {
                    double amount = bounty.getAmount();
                    RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

                    if (rsp != null && amount > 0 && issuer != null) {
                        Economy economy = rsp.getProvider();
                        economy.depositPlayer(issuer, amount);

                        player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_REFUNDED.get(),
                            "%amount%", String.valueOf(amount),
                            "%issuer%", issuer
                        ));
                    }
                }

                String targetName = bounty.getTargetName() != null ? bounty.getTargetName() : "Unknown";

                plugin.getBountyStorage().deleteBounty(id).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_REMOVED.get(),
                            "%id%", String.valueOf(id),
                            "%player%", targetName
                        ));

                        Bukkit.broadcast(Colors.color(Settings.LangKey.BOUNTY_REMOVE_BROADCAST.get(),
                            "%id%", String.valueOf(id),
                            "%player%", targetName,
                            "%issuer%", player.getName()
                        ));
                    });
                });
            });
        });
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        return Collections.emptyList();
    }
}