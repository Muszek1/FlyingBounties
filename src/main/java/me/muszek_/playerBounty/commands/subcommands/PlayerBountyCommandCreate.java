package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.utils.TabCompletePlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PlayerBountyCommandCreate extends SubCommand {

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Wystawia zlecenie na zabójstwo gracza";
    }

    @Override
    public String getSyntax() {
        return "/bounty create <gracz> <kwota>";
    }

    @Override
    public String getPermission() {
        return "playerbounty.wystaw";
    }

    @Override
    public void perform(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_CREATE_USAGE.get()));
            return;
        }

        PlayerBounty plugin = PlayerBounty.getInstance();

        Economy economy = Bukkit.getServicesManager()
                .getRegistration(Economy.class)
                .getProvider();

        OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(args[1]);
        if (!targetOffline.hasPlayedBefore() && !targetOffline.isOnline()) {
            player.sendMessage(Colors.color(
                    Settings.LangKey.PLAYER_NOT_FOUND.get().replace("%player%", args[1])
            ));
            return;
        }
        String targetName = targetOffline.getName();
        UUID targetUuid = targetOffline.getUniqueId();

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            player.sendMessage(Colors.color(Settings.LangKey.WRONG_NUMBER.get()));
            return;
        }

        FileConfiguration checkCfg = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "bounties.yml")
        );
        int activeCount = 0;
        if (checkCfg.contains("bounties")) {
            for (String key : checkCfg.getConfigurationSection("bounties").getKeys(false)) {
                if (player.getName().equals(checkCfg.getString("bounties." + key + ".issuer"))) {
                    activeCount++;
                }
            }
        }
        int defaultLimit = plugin.getConfig().getInt(
                "bounty-default-limit",
                (int) Settings.ConfigKey.BOUNTY_LIMIT.get()
        );
        int permissionLimit = 0;
        for (var perm : player.getEffectivePermissions()) {
            String node = perm.getPermission();
            if (node.startsWith("zlecenia.limit.")) {
                String suffix = node.substring("zlecenia.limit.".length());
                try {
                    permissionLimit = Math.max(permissionLimit, Integer.parseInt(suffix));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        boolean unlimited = player.hasPermission("playerbounty.limit.unlimited");
        int finalLimit = unlimited ? Integer.MAX_VALUE : Math.max(defaultLimit, permissionLimit);
        if (activeCount >= finalLimit) {
            player.sendMessage(Colors.color(
                    Settings.LangKey.BOUNTY_LIMIT_REACHED.get()
                            .replace("%limit%", String.valueOf(finalLimit))
            ));
            return;
        }

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        File bountyFile = new File(dataFolder, "bounties.yml");
        if (!bountyFile.exists()) {
            try {
                bountyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_SAVE_ERROR.get()));
                return;
            }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(bountyFile);

        if (economy.getBalance(player) < amount) {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_NOT_ENOUGH_MONEY.get()));
            return;
        }
        economy.withdrawPlayer(player, amount);

        int lastId = cfg.getInt("last-id", 0);
        int newId = lastId + 1;
        cfg.set("last-id", newId);
        String path = "bounties." + newId;
        cfg.set(path + ".issuer", player.getName());
        cfg.set(path + ".target-name", targetName);
        cfg.set(path + ".target-uuid", targetUuid.toString());
        cfg.set(path + ".amount", amount);

        int minutes = plugin.getConfig().getInt(
                "bounty-time",
                (int) Settings.ConfigKey.BOUNTY_TIME.get()
        );
        long durationSeconds = (long) minutes * 60L;
        Instant now = Instant.now();
        Instant expires = now.plusSeconds(durationSeconds);
        cfg.set(path + ".created", now.toString());
        cfg.set(path + ".expires", expires.toString());

        try {
            cfg.save(bountyFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_SAVE_ERROR.get()));
            return;
        }

        player.sendMessage(Colors.color(
                Settings.LangKey.BOUNTY_CREATED.get()
                        .replace("%player%", targetName)
                        .replace("%price%", String.valueOf(amount))
        ));
        String msg = Settings.LangKey.BOUNTY_BROADCAST.get()
                .replace("%id%", String.valueOf(newId))
                .replace("%player%", targetName)
                .replace("%price%", String.valueOf(amount))
                .replace("%issuer%", player.getName());
        Bukkit.broadcastMessage(Colors.color(msg));
    }

    @Override
    public List<String> getSubcommandArguments(Player player, String[] args) {
        if (args.length == 2) {
            return TabCompletePlayer.getOnlinePlayerNames();
        }
        return null;
    }
}