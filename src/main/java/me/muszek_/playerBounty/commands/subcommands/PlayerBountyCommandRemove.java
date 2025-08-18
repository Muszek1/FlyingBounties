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
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_NOT_FOUND.get().replace("%id%", String.valueOf(id))));
            return;
        }

        String issuer = cfg.getString(path + ".issuer");
        boolean isAdmin = player.hasPermission("zlecenia.admin");
        if (!player.getName().equals(issuer) && !isAdmin) {
            player.sendMessage(Colors.color(Settings.LangKey.NO_PERMISSION.get()));
            return;
        }

        double amount = cfg.getDouble(path + ".amount", 0.0);
        Economy economy = Bukkit.getServicesManager()
                .getRegistration(Economy.class)
                .getProvider();
        if (economy != null && amount > 0) {
            economy.depositPlayer(issuer, amount);
            player.sendMessage(Colors.color("&aZwrócono &e" + amount + " &ado konta gracza &6" + issuer));
        }

        String target = cfg.getString(path + ".target");
        cfg.set(path, null);

        try {
            cfg.save(bountyFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(Colors.color("Błąd zapisu do pliku!"));
            return;
        }

        player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_REMOVED.get()
                .replace("%id%", String.valueOf(id))
                .replace("%player%", target)
        ));
        Bukkit.broadcastMessage(Colors.color(
                Settings.LangKey.BOUNTY_REMOVE_BROADCAST.get()
                        .replace("%id%", String.valueOf(id))
                        .replace("%player%", target)
                        .replace("%issuer%", player.getName())
        ));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            File bountyFile = new File(PlayerBounty.getInstance().getDataFolder(), "bounties.yml");
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);
            if (cfg.contains("bounties")) {
                return cfg.getConfigurationSection("bounties").getKeys(false).stream().toList();
            }
        }
        return null;
    }
}
