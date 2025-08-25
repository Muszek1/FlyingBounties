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
        return "usun";
    }

    @Override
    public String getDescription() {
        return "Usuwa zlecenie, jeśli jesteś autorem lub masz uprawnienia administracyjne";
    }

    @Override
    public String getSyntax() {
        return "/bounty usun <id>";
    }

    @Override
    public String getPermission() {
        return "zlecenia.usun";
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

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
            String msg = nz(Settings.LangKey.BOUNTY_NOT_FOUND.get())
                    .replace("%id%", String.valueOf(id));
            player.sendMessage(Colors.color(msg));
            return;
        }

        String issuer = cfg.getString(path + ".issuer");
        boolean isAdmin = player.hasPermission("zlecenia.admin");
        if (!(player.getName().equals(issuer) || isAdmin)) {
            player.sendMessage(Colors.color(nz(Settings.LangKey.NO_PERMISSION.get())));
            return;
        }

        double amount = cfg.getDouble(path + ".amount", 0.0);
        String target = cfg.getString(path + ".target-name");


        cfg.set(path, null);
        try {
            cfg.save(bountyFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(Colors.color("&cBłąd zapisu do pliku!"));
            return;
        }

        Economy economy = null;
        try {
            var reg = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (reg != null) economy = reg.getProvider();
        } catch (Exception ignored) {}

        if (economy != null && amount > 0) {
            if (issuer != null && !issuer.isEmpty()) {
                economy.depositPlayer(issuer, amount);
                player.sendMessage(Colors.color(
                        Settings.LangKey.BOUNTY_REFUNDED.get()
                                .replace("%amount%", String.valueOf(amount))
                                .replace("%issuer%", issuer)
                ));
            } else {
                player.sendMessage(Colors.color("&eUwaga: nie udało się zwrócić nagrody, bo wystawca jest nieznany."));
            }
        }

        String removedMsg = nz(Settings.LangKey.BOUNTY_REMOVED.get())
                .replace("%id%", String.valueOf(id))
                .replace("%player%", nz(target));
        player.sendMessage(Colors.color(removedMsg));

        String broadcast = nz(Settings.LangKey.BOUNTY_REMOVE_BROADCAST.get())
                .replace("%id%", String.valueOf(id))
                .replace("%player%", nz(target))
                .replace("%issuer%", player.getName());
        Bukkit.broadcastMessage(Colors.color(broadcast));
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
