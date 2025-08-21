package me.muszek_.playerBounty.menusystem;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
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
        int contentRows = Math.max(rows - 1, 1); // ostatni rząd na kontrolki, minimum 1 rząd zawartości
        int maxPerPage = contentRows * 9;

        int totalPages = (keys.size() + maxPerPage - 1) / maxPerPage;
        if (totalPages < 1) totalPages = 1;

        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String rawTitle = menuConfig.getString("menu.title", "Zlecenia");
        String pageFormat = menuConfig.getString("menu.pagination-format", "%title% (%page%/%total%)");

        String formattedTitle = pageFormat
                .replace("%title%", rawTitle)
                .replace("%page%", String.valueOf(page + 1))
                .replace("%total%", String.valueOf(totalPages));

        String title = Colors.color(formattedTitle);
        Inventory inv = Bukkit.createInventory(null, size, title);

        int fromIndex = page * maxPerPage;
        int toIndex = Math.min(fromIndex + maxPerPage, keys.size());
        List<String> pageKeys = keys.subList(fromIndex, toIndex);

        // Wymuszamy główkę gracza dla pozycji zleceń
        Material mat = Material.PLAYER_HEAD;

        String nameT = menuConfig.getString("menu.items.bounties.name", "Zlecenie #%id%");
        List<String> loreT = menuConfig.getStringList("menu.items.bounties.lore");

        String tzString = plugin.getConfig().getString("settings.timezone", "UTC");
        ZoneId zone;
        try {
            zone = ZoneId.of(tzString);
        } catch (Exception ex) {
            zone = ZoneId.systemDefault();
        }
        DateTimeFormatter fmt = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm")
                .withZone(zone);

        for (int i = 0; i < pageKeys.size(); i++) {
            String key = pageKeys.get(i);

            String issuer = bcfg.getString("bounties." + key + ".issuer", "?");
            String target = bcfg.getString("bounties." + key + ".target-name", "?");
            double amount = bcfg.getDouble("bounties." + key + ".amount", 0.0D);
            String expiresRaw = bcfg.getString("bounties." + key + ".expires", "");

            String expires = expiresRaw;
            try {
                Instant inst = Instant.parse(expiresRaw);
                expires = fmt.format(inst); // używamy wybranej strefy czasowej
            } catch (Exception ignored) {
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();

            // Ustawiamy nazwę
            meta.setDisplayName(Colors.color(nameT.replace("%id%", key)));

            // Ustawiamy lore
            String finalExpires = expires;
            List<String> lore = loreT.stream()
                    .map(line -> Colors.color(line
                            .replace("%id%", key)
                            .replace("%issuer%", issuer)
                            .replace("%target%", target)
                            .replace("%amount%", String.valueOf(amount))
                            .replace("%expires%", finalExpires)
                    ))
                    .collect(Collectors.toList());
            meta.setLore(lore);

            // Jeśli to główka – ustawiamy właściciela na target
            if (meta instanceof SkullMeta) {
                SkullMeta skull = (SkullMeta) meta;
                // Gdy target jest znany i nie jest placeholderem
                if (target != null && !target.isEmpty() && !target.equals("?")) {
                    OfflinePlayer off = Bukkit.getOfflinePlayer(target);
                    // setOwningPlayer działa dla 1.13+
                    skull.setOwningPlayer(off);
                }
                item.setItemMeta(skull);
            } else {
                item.setItemMeta(meta);
            }

            inv.setItem(i, item);
        }

        // Sekcja kontrolek (nawigacja itd.)
        ConfigurationSection controls = menuConfig.getConfigurationSection("menu.controls");
        if (controls != null) {
            for (String ckey : controls.getKeys(false)) {
                ConfigurationSection cs = controls.getConfigurationSection(ckey);
                if (cs == null) continue;

                int ctrlSlot = cs.getInt("slot", -1);
                if (ctrlSlot < 0 || ctrlSlot >= size) continue;

                Material matCtrl;
                try {
                    matCtrl = Material.valueOf(cs.getString("material", "ARROW").toUpperCase());
                } catch (Exception e) {
                    matCtrl = Material.ARROW;
                }

                ItemStack btn = new ItemStack(matCtrl);
                ItemMeta bm = btn.getItemMeta();
                if (bm != null) {
                    bm.setDisplayName(Colors.color(cs.getString("name", ckey)));
                    List<String> btnLore = cs.getStringList("lore").stream()
                            .map(Colors::color).collect(Collectors.toList());
                    bm.setLore(btnLore);
                    btn.setItemMeta(bm);
                }
                inv.setItem(ctrlSlot, btn);
            }
        }

        player.openInventory(inv);
    }
}
