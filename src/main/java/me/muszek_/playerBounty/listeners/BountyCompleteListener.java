package me.muszek_.playerBounty.listeners;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.settings.Settings;
import me.muszek_.playerBounty.storage.Bounty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BountyCompleteListener implements Listener {
    private final PlayerBounty plugin;

    public BountyCompleteListener(PlayerBounty plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        boolean stacked = Settings.ConfigKey.BOUNTY_STACKED.<Boolean>get();
        UUID victimId = victim.getUniqueId();

        plugin.getBountyStorage().getBountiesByTarget(victimId).thenAccept(matches -> {
            if (matches.isEmpty()) return;

            Bukkit.getScheduler().runTask(plugin, () -> {
                List<Bounty> toProcess;
                if (stacked) {
                    toProcess = new ArrayList<>(matches);
                } else {
                    toProcess = matches.stream()
                        .min(Comparator.comparing(Bounty::getCreated))
                        .map(List::of)
                        .orElse(new ArrayList<>());
                }

                if (toProcess.isEmpty()) return;

                double totalReward = 0;
                List<String> itemRewardsNames = new ArrayList<>();
                List<String> processedIds = new ArrayList<>();

                for (Bounty bounty : toProcess) {
                    if (bounty.isItemReward()) {
                        ItemStack item = bounty.getRewardItem();
                        if (item != null) {
                            killer.getInventory().addItem(item).forEach((idx, left) ->
                                killer.getWorld().dropItem(killer.getLocation(), left));

                            String itemName = item.getType().name();
                            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                                itemName = item.getItemMeta().getDisplayName();
                            }
                            itemRewardsNames.add(itemName + " x" + item.getAmount());
                        }
                    } else {
                        totalReward += bounty.getAmount();
                    }

                    plugin.getBountyStorage().deleteBounty(bounty.getId());
                    processedIds.add(String.valueOf(bounty.getId()));
                }

                if (totalReward > 0) {
                    double finalTotalReward = totalReward;
                    plugin.getServer().dispatchCommand(
                        Bukkit.getConsoleSender(),
                        String.format("eco give %s %s", killer.getName(), finalTotalReward)
                    );
                }

                String idPart = String.join(",", processedIds);

                String rewardString;
                if (totalReward > 0 && !itemRewardsNames.isEmpty()) {
                    rewardString = totalReward + "$ + " + String.join(", ", itemRewardsNames);
                } else if (totalReward > 0) {
                    rewardString = totalReward + "$";
                } else {
                    rewardString = String.join(", ", itemRewardsNames);
                }

                Bukkit.broadcast(Colors.color(
                    Settings.LangKey.BOUNTY_COMPLETE.get(),
                    "%id%", idPart,
                    "%player%", victim.getName(),
                    "%killer%", killer.getName(),
                    "%amount%", rewardString
                ));
            });
        });
    }
}