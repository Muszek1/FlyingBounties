package me.muszek_.playerBounty.commands.subcommands;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.commands.SubCommand;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.utils.TabCompletePlayer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

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

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(Colors.color(Settings.LangKey.PLAYER_NOT_FOUND.get(), "%player%", args[1]));
            return;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_SELF_NOT_ALLOWED.get(), "%player%", player.getName()));
            return;
        }

        double amount = 0;
        ItemStack itemReward = null;
        boolean isItem = args[2].equalsIgnoreCase("item");

        if (isItem) {
            itemReward = player.getInventory().getItemInMainHand();
            if (itemReward == null || itemReward.getType() == Material.AIR) {
                player.sendMessage(Colors.color("&cMusisz trzymać przedmiot w ręce!"));
                return;
            }
            itemReward = itemReward.clone();
        } else {
            try {
                amount = Double.parseDouble(args[2]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                player.sendMessage(Colors.color(Settings.LangKey.WRONG_NUMBER.get()));
                return;
            }

            Economy economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
            if (!economy.has(player, amount)) {
                player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_NOT_ENOUGH_MONEY.get()));
                return;
            }
        }

        PlayerBounty plugin = PlayerBounty.getInstance();
        File bountyFile = new File(plugin.getDataFolder(), "bounties.yml");
        FileConfiguration bountyCfg = YamlConfiguration.loadConfiguration(bountyFile);

        if (!checkLimit(player, plugin, bountyCfg)) {
            return;
        }

        if (isItem) {
            player.getInventory().setItemInMainHand(null);
        } else {
            Economy economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
            economy.withdrawPlayer(player, amount);
        }

        int newId = bountyCfg.getInt("last-id", 0) + 1;
        bountyCfg.set("last-id", newId);

        String path = "bounties." + newId;
        long durationSeconds = plugin.getConfig().getInt("bounty-time", (int) Settings.ConfigKey.BOUNTY_TIME.get()) * 60L;
        Instant now = Instant.now();

        bountyCfg.set(path + ".issuer", player.getName());
        bountyCfg.set(path + ".target-name", target.getName());
        bountyCfg.set(path + ".target-uuid", target.getUniqueId().toString());
        bountyCfg.set(path + ".created", now.toString());
        bountyCfg.set(path + ".expires", now.plusSeconds(durationSeconds).toString());

        String displayPrice;
        if (isItem) {
            bountyCfg.set(path + ".reward-item", itemReward);
            bountyCfg.set(path + ".amount", 0);
            displayPrice = itemReward.getType().name();
            if (itemReward.hasItemMeta() && itemReward.getItemMeta().hasDisplayName()) {
                displayPrice = itemReward.getItemMeta().getDisplayName();
            } else {
                displayPrice = displayPrice + " x" + itemReward.getAmount();
            }
        } else {
            bountyCfg.set(path + ".amount", amount);
            displayPrice = String.valueOf(amount);
        }

        try {
            bountyCfg.save(bountyFile);
        } catch (IOException e) {
            e.printStackTrace();
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_SAVE_ERROR.get()));
            if (!isItem) {
                Economy economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
                economy.depositPlayer(player, amount);
            } else {
                player.getInventory().addItem(itemReward);
            }
            return;
        }

        if (isItem) {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_CREATED_ITEM.get(),
                "%player%", target.getName(),
                "%item%", displayPrice
            ));

            Bukkit.broadcast(Colors.color(Settings.LangKey.BOUNTY_BROADCAST_ITEM.get(),
                "%id%", String.valueOf(newId),
                "%player%", target.getName(),
                "%item%", displayPrice,
                "%issuer%", player.getName()
            ));
        } else {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_CREATED.get(),
                "%player%", target.getName(),
                "%price%", displayPrice
            ));

            Bukkit.broadcast(Colors.color(Settings.LangKey.BOUNTY_BROADCAST.get(),
                "%id%", String.valueOf(newId),
                "%player%", target.getName(),
                "%price%", displayPrice,
                "%issuer%", player.getName()
            ));
        }
    }

    private boolean checkLimit(Player player, PlayerBounty plugin, FileConfiguration bountyCfg) {
        if (player.hasPermission("flyingbounties.limit.unlimited")) return true;

        int activeCount = 0;
        ConfigurationSection bountiesSection = bountyCfg.getConfigurationSection("bounties");
        if (bountiesSection != null) {
            for (String key : bountiesSection.getKeys(false)) {
                if (player.getName().equals(bountiesSection.getString(key + ".issuer"))) {
                    activeCount++;
                }
            }
        }

        int limit = plugin.getConfig().getInt("bounty-default-limit", (int) Settings.ConfigKey.BOUNTY_LIMIT.get());

        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            String perm = pai.getPermission();
            if (perm.startsWith("flyingbounties.limit.")) {
                try {
                    int customLimit = Integer.parseInt(perm.substring("flyingbounties.limit.".length()));
                    limit = Math.max(limit, customLimit);
                } catch (NumberFormatException ignored) {}
            }
        }

        if (activeCount >= limit) {
            player.sendMessage(Colors.color(Settings.LangKey.BOUNTY_LIMIT_REACHED.get(), "%limit%", String.valueOf(limit)));
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