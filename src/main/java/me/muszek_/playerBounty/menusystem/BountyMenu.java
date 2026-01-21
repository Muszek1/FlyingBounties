package me.muszek_.playerBounty.menusystem;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.storage.Bounty;
import me.muszek_.playerBounty.utils.MaterialUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BountyMenu {
    private final PlayerBounty plugin;
    private File menuFile;
    private YamlConfiguration menuConfig;

    public BountyMenu(PlayerBounty plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        menuFile = new File(plugin.getDataFolder(), "menu.yml");
        if (!menuFile.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        loadConfig();

        // Pobieramy dane asynchronicznie
        plugin.getBountyStorage().getAllBounties().thenAccept(allBounties -> {
            // Wracamy na główny wątek, by zbudować inventory
            Bukkit.getScheduler().runTask(plugin, () -> {
                int size = menuConfig.getInt("menu.size", 36);
                int rows = size / 9;
                int contentRows = rows - 1;
                int maxPerPage = contentRows * 9;

                int totalPages = (allBounties.size() + maxPerPage - 1) / maxPerPage;
                if (totalPages < 1) totalPages = 1;

                int finalPage = page;
                if (finalPage < 0) finalPage = 0;
                if (finalPage >= totalPages) finalPage = totalPages - 1;

                String rawTitle = menuConfig.getString("menu.title", "Zlecenia");
                String pageFormat = menuConfig.getString("menu.pagination-format", "%title% (%page%/%total%)");

                Component title = Colors.color(pageFormat,
                    "%title%", rawTitle,
                    "%page%", String.valueOf(finalPage + 1),
                    "%total%", String.valueOf(totalPages)
                );

                Inventory inv = Bukkit.createInventory(null, size, title);

                int fromIndex = finalPage * maxPerPage;
                int toIndex = Math.min(fromIndex + maxPerPage, allBounties.size());
                List<Bounty> pageBounties = allBounties.subList(fromIndex, toIndex);

                String nameT = menuConfig.getString("menu.items.bounties.name", "Zlecenie #%id%");
                List<String> loreMoney = menuConfig.getStringList("menu.items.bounties.lore-money");
                List<String> loreItem = menuConfig.getStringList("menu.items.bounties.lore-item");

                if (loreMoney.isEmpty() && menuConfig.contains("menu.items.bounties.lore")) {
                    loreMoney = menuConfig.getStringList("menu.items.bounties.lore");
                }
                if (loreItem.isEmpty()) {
                    loreItem = loreMoney;
                }

                String tzString = plugin.getConfig().getString("settings.timezone", "UTC");
                ZoneId zone;
                try {
                    zone = ZoneId.of(tzString);
                } catch (Exception ex) {
                    zone = ZoneId.systemDefault();
                }
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone);

                for (int i = 0; i < pageBounties.size(); i++) {
                    Bounty bounty = pageBounties.get(i);

                    String amountStr;
                    ItemStack item;
                    List<String> activeLoreTemplate;

                    if (bounty.isItemReward()) {
                        activeLoreTemplate = loreItem;
                        ItemStack rewardItem = bounty.getRewardItem();
                        if (rewardItem != null) {
                            item = rewardItem.clone();
                            if (rewardItem.hasItemMeta() && rewardItem.getItemMeta().hasDisplayName()) {
                                amountStr = rewardItem.getItemMeta().getDisplayName();
                            } else {
                                amountStr = formatMaterialName(rewardItem.getType()) + " x" + rewardItem.getAmount();
                            }
                        } else {
                            item = new ItemStack(Material.BARRIER);
                            amountStr = "Error Item";
                        }
                    } else {
                        activeLoreTemplate = loreMoney;
                        Material headMat = MaterialUtils.getPlayerHead();
                        if (headMat.name().equals("SKULL_ITEM")) {
                            item = new ItemStack(headMat, 1, (short) 3);
                        } else {
                            item = new ItemStack(headMat, 1);
                        }
                        amountStr = String.valueOf(bounty.getAmount());
                    }

                    String expires = "";
                    try {
                        expires = fmt.format(bounty.getExpires());
                    } catch (Exception ignored) {}

                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (!bounty.isItemReward() && meta instanceof SkullMeta) {
                            OfflinePlayer offPlayer = null;
                            if (bounty.getTargetUuid() != null) {
                                offPlayer = Bukkit.getOfflinePlayer(bounty.getTargetUuid());
                            }
                            if (offPlayer == null && bounty.getTargetName() != null) {
                                offPlayer = Bukkit.getOfflinePlayer(bounty.getTargetName());
                            }
                            if (offPlayer != null) {
                                ((SkullMeta) meta).setOwningPlayer(offPlayer);
                            }
                        }

                        meta.displayName(Colors.color(nameT, "%id%", String.valueOf(bounty.getId())));

                        String finalExpires = expires;
                        String finalAmountStr = amountStr;

                        List<Component> lore = activeLoreTemplate.stream()
                            .map(line -> Colors.color(line,
                                "%id%", String.valueOf(bounty.getId()),
                                "%issuer%", bounty.getIssuer() != null ? bounty.getIssuer() : "?",
                                "%target%", bounty.getTargetName() != null ? bounty.getTargetName() : "?",
                                "%amount%", finalAmountStr,
                                "%item%", finalAmountStr,
                                "%expires%", finalExpires
                            ))
                            .collect(java.util.stream.Collectors.toList());

                        meta.lore(lore);
                        item.setItemMeta(meta);
                    }
                    inv.setItem(i, item);
                }

                loadControls(inv);
                loadCustomItems(inv);

                player.openInventory(inv);
            });
        });
    }

    private void loadControls(Inventory inv) {
        ConfigurationSection controls = menuConfig.getConfigurationSection("menu.controls");
        if (controls != null) {
            for (String key : controls.getKeys(false)) {
                ConfigurationSection cs = controls.getConfigurationSection(key);
                if (cs == null) continue;

                int ctrlSlot = cs.getInt("slot");
                Material matCtrl = Material.getMaterial(cs.getString("material", "ARROW").toUpperCase());
                if (matCtrl == null) matCtrl = Material.ARROW;

                ItemStack btn = new ItemStack(matCtrl);
                ItemMeta bm = btn.getItemMeta();

                bm.displayName(Colors.color(cs.getString("name", key)));

                List<Component> btnLore = cs.getStringList("lore").stream()
                    .map(Colors::color)
                    .collect(java.util.stream.Collectors.toList());
                bm.lore(btnLore);

                btn.setItemMeta(bm);
                inv.setItem(ctrlSlot, btn);
            }
        }
    }

    private void loadCustomItems(Inventory inv) {
        ConfigurationSection custom = menuConfig.getConfigurationSection("menu.custom");
        if (custom != null) {
            for (String key : custom.getKeys(false)) {
                ConfigurationSection cs = custom.getConfigurationSection(key);
                if (cs == null) continue;

                int slot = cs.getInt("slot");
                Material matCustom = Material.getMaterial(cs.getString("material", "STONE").toUpperCase());
                if (matCustom == null) matCustom = Material.STONE;

                ItemStack it = new ItemStack(matCustom);
                ItemMeta im = it.getItemMeta();

                if (matCustom == MaterialUtils.getPlayerHead() && im instanceof SkullMeta) {
                    SkullMeta sm = (SkullMeta) im;
                    String owner = cs.getString("skull-owner", "");
                    if (owner != null && !owner.isEmpty()) {
                        sm.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
                    }
                }

                im.displayName(Colors.color(cs.getString("name", key)));

                List<Component> l = cs.getStringList("lore").stream()
                    .map(Colors::color)
                    .collect(java.util.stream.Collectors.toList());
                im.lore(l);

                it.setItemMeta(im);
                inv.setItem(slot, it);
            }
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        if (name.isEmpty()) return name;

        StringBuilder sb = new StringBuilder();
        boolean capitalize = true;
        for (char c : name.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalize = true;
                sb.append(c);
            } else if (capitalize) {
                sb.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}