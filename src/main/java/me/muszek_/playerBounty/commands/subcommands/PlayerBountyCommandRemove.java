package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        File bountyFile = new File(PlayerBounty.getInstance().getDataFolder(), "bounties.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);

        String path = "bounties." + id;
        if (!cfg.contains(path)) {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_NOT_FOUND.get(), "%id%", String.valueOf(id)));
            return;
        }

        String issuer = cfg.getString(path + ".issuer");
        boolean isAdmin = player.hasPermission("flyingbounties.admin");

        if (!isAdmin && (issuer == null || !issuer.equals(player.getName()))) {
            player.sendMessage(Colors.color(Settings.LangKey.NO_PERMISSION.get()));
            return;
        }

        if (cfg.contains(path + ".reward-item")) {
            ItemStack item = cfg.getItemStack(path + ".reward-item");
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
            double amount = cfg.getDouble(path + ".amount", 0.0);
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

        String targetName = cfg.getString(path + ".target-name", "Unknown");

        cfg.set(path, null);
        try {
            cfg.save(bountyFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(Colors.color("&cError saving file!"));
            return;
        }

        player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_REMOVED.get(),
            "%id%", String.valueOf(id),
            "%player%", targetName
        ));

        Bukkit.broadcast(Colors.color(Settings.LangKey.BOUNTY_REMOVE_BROADCAST.get(),
            "%id%", String.valueOf(id),
            "%player%", targetName,
            "%issuer%", player.getName()
        ));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            File bountyFile = new File(PlayerBounty.getInstance().getDataFolder(), "bounties.yml");
            if (bountyFile.exists()) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);
                ConfigurationSection section = cfg.getConfigurationSection("bounties");
                if (section != null) {
                    return new ArrayList<>(section.getKeys(false));
                }
            }
        }
        return Collections.emptyList();
    }
}