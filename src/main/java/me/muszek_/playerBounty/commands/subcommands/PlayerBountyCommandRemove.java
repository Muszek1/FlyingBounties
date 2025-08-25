package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PlayerBountyCommandRemove extends SubCommand {

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Usuwa zlecenie, jeśli jesteś autorem lub masz uprawnienia administracyjne";
    }

    @Override
    public String getSyntax() {
        return "/bounty delete <id>";
    }

    @Override
    public String getPermission() {
        return "playerbounty.usun";
    }

    private static String nz(String s) { return s == null ? "" : s; }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Colors.color(nz(Settings.LangKey.BOUNTY_REMOVE_USAGE.get())));
            return;
        }

        final int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage(Colors.color(nz(Settings.LangKey.WRONG_NUMBER.get())));
            return;
        }

        File bountyFile = new File(PlayerBounty.getInstance().getDataFolder(), "bounties.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);

        String path = "bounties." + id;
        if (!cfg.contains(path)) {
            player.sendMessage(Colors.color(nz(Settings.LangKey.BOUNTY_NOT_FOUND.get()).replace("%id%", String.valueOf(id))));
            return;
        }

        String issuer = cfg.getString(path + ".issuer");
        boolean isAdmin = player.hasPermission("playerbounty.admin");
        if (!player.getName().equals(issuer) && !isAdmin) {
            player.sendMessage(Colors.color(nz(Settings.LangKey.NO_PERMISSION.get())));
            return;
        }

        double amount = cfg.getDouble(path + ".amount", 0.0);
        Economy economy = Bukkit.getServicesManager().getRegistration(Economy.class) != null
                ? Bukkit.getServicesManager().getRegistration(Economy.class).getProvider()
                : null;
        if (economy != null && amount > 0 && issuer != null && !issuer.isEmpty()) {
            economy.depositPlayer(issuer, amount);
            String refundMsg = nz(Settings.LangKey.BOUNTY_REFUNDED.get())
                    .replace("%issuer%", nz(issuer))
                    .replace("%amount%", String.valueOf(amount));
            player.sendMessage(Colors.color(refundMsg));
        }

        String target = cfg.getString(path + ".target-name");
        cfg.set(path, null);

        try {
            cfg.save(bountyFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(Colors.color(nz(Settings.LangKey.BOUNTY_SAVE_ERROR.get())));
            return;
        }

        player.sendMessage(Colors.color(nz(Settings.LangKey.BOUNTY_REMOVED.get())
                .replace("%id%", String.valueOf(id))
                .replace("%player%", nz(target))
        ));
        Bukkit.broadcastMessage(Colors.color(
                nz(Settings.LangKey.BOUNTY_REMOVE_BROADCAST.get())
                        .replace("%id%", String.valueOf(id))
                        .replace("%player%", nz(target))
                        .replace("%issuer%", player.getName())
        ));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            File bountyFile = new File(PlayerBounty.getInstance().getDataFolder(), "bounties.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);
            if (cfg.contains("bounties") && cfg.getConfigurationSection("bounties") != null) {
                return cfg.getConfigurationSection("bounties").getKeys(false).stream().toList();
            }
        }
        return Collections.emptyList();
    }
}
