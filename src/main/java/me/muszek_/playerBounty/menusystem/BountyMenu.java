package me.muszek_.playerBounty.menusystem;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

        File bFile = new File(plugin.getDataFolder(), "bounties.yml");
        YamlConfiguration bcfg = YamlConfiguration.loadConfiguration(bFile);

        List<String> keys = Optional.ofNullable(bcfg.getConfigurationSection("bounties"))
            .map(sec -> new ArrayList<>(sec.getKeys(false)))
            .orElseGet(ArrayList::new);

        int size = menuConfig.getInt("menu.size", 36);
        int rows = size / 9;
        int contentRows = rows - 1;
        int maxPerPage = contentRows * 9;

        int totalPages = (keys.size() + maxPerPage - 1) / maxPerPage;
        if (totalPages < 1) totalPages = 1;

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String rawTitle = menuConfig.getString("menu.title", "Zlecenia");
        String pageFormat = menuConfig.getString("menu.pagination-format", "%title% (%page%/%total%)");

        Component title = Colors.color(pageFormat,
            "%title%", rawTitle,
            "%page%", String.valueOf(page + 1),
            "%total%", String.valueOf(totalPages)
        );

        Inventory inv = Bukkit.createInventory(null, size, title);

        int fromIndex = page * maxPerPage;
        int toIndex = Math.min(fromIndex + maxPerPage, keys.size());
        List<String> pageKeys = keys.subList(fromIndex, toIndex);

        String nameT = menuConfig.getString("menu.items.bounties.name", "Zlecenie #%id%");

        // Pobieramy obie listy lore
        List<String> loreMoney = menuConfig.getStringList("menu.items.bounties.lore-money");
        List<String> loreItem = menuConfig.getStringList("menu.items.bounties.lore-item");

        // Fallback dla kompatybilności wstecznej
        if (loreMoney.isEmpty() && menuConfig.contains("menu.items.bounties.lore")) {
            loreMoney = menuConfig.getStringList("menu.items.bounties.lore");
        }
        if (loreItem.isEmpty()) {
            loreItem = loreMoney; // Domyślnie to samo jeśli nie ustawiono lore-item
        }

        String tzString = plugin.getConfig().getString("settings.timezone", "UTC");
        ZoneId zone;
        try {
            zone = ZoneId.of(tzString);
        } catch (Exception ex) {
            zone = ZoneId.systemDefault();
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone);

        for (int i = 0; i < pageKeys.size(); i++) {
            String key = pageKeys.get(i);
            String path = "bounties." + key;

            String issuer = bcfg.getString(path + ".issuer", "?");
            String targetName = bcfg.getString(path + ".target-name", "?");
            String targetUuidStr = bcfg.getString(path + ".target-uuid");
            double amount = bcfg.getDouble(path + ".amount", 0);
            String expiresRaw = bcfg.getString(path + ".expires", "");

            String amountStr;
            boolean isItemReward = false;
            ItemStack item;
            List<String> activeLoreTemplate;

            if (bcfg.contains(path + ".reward-item")) {
                isItemReward = true;
                activeLoreTemplate = loreItem;
                ItemStack rewardItem = bcfg.getItemStack(path + ".reward-item");
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
                // Obsługa starszych wersji (1.8-1.12), gdzie głowa gracza to SKULL_ITEM z data 3
                if (headMat.name().equals("SKULL_ITEM")) {
                    item = new ItemStack(headMat, 1, (short) 3);
                } else {
                    item = new ItemStack(headMat, 1);
                }
                amountStr = String.valueOf(amount);
            }

            String expires = expiresRaw;
            try {
                if (!expiresRaw.isEmpty()) {
                    expires = fmt.format(Instant.parse(expiresRaw));
                }
            } catch (Exception ignored) {}

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Ustawianie główki tylko jeśli to nie jest nagroda przedmiotowa
                if (!isItemReward && meta instanceof SkullMeta) {
                    OfflinePlayer offPlayer = null;
                    if (targetUuidStr != null) {
                        try {
                            offPlayer = Bukkit.getOfflinePlayer(UUID.fromString(targetUuidStr));
                        } catch (Exception ignored) {}
                    }
                    if (offPlayer == null && targetName != null && !targetName.equals("?")) {
                        offPlayer = Bukkit.getOfflinePlayer(targetName);
                    }

                    if (offPlayer != null) {
                        ((SkullMeta) meta).setOwningPlayer(offPlayer);
                    }
                }

                meta.displayName(Colors.color(nameT, "%id%", key));

                String finalExpires = expires;
                String finalAmountStr = amountStr;

                List<Component> lore = activeLoreTemplate.stream()
                    .map(line -> Colors.color(line,
                        "%id%", key,
                        "%issuer%", issuer != null ? issuer : "",
                        "%target%", targetName != null ? targetName : "",
                        "%amount%", finalAmountStr,
                        "%item%", finalAmountStr, // Alias dla wygody
                        "%expires%", finalExpires != null ? finalExpires : ""
                    ))
                    .collect(Collectors.toList());

                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        setupControls(inv, controls -> {
        });

        loadControls(inv);
        loadCustomItems(inv);

        player.openInventory(inv);
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
                    .collect(Collectors.toList());
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
                    .collect(Collectors.toList());
                im.lore(l);

                it.setItemMeta(im);
                inv.setItem(slot, it);
            }
        }
    }

    private void setupControls(Inventory inv, java.util.function.Consumer<Void> action) {}

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