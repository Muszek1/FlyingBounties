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
        int contentRows = rows - 1;
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
            double amount = bcfg.getDouble("bounties." + key + ".amount", 0);
            String expiresRaw = bcfg.getString("bounties." + key + ".expires", "");
            String expires = expiresRaw;
            try {
                Instant inst = Instant.parse(expiresRaw);
                expires = fmt.format(inst);
            } catch (Exception ignored) {}

            ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (target != null && !target.isEmpty() && !target.equals("?")) {
                OfflinePlayer offp = Bukkit.getOfflinePlayer(target);
                meta.setOwningPlayer(offp);
            }

            meta.setDisplayName(Colors.color(nameT.replace("%id%", key)));

            String finalExpires = expires;
            List<String> lore = loreT.stream()
                    .map(line -> Colors.color(line
                            .replace("%id%", key)
                            .replace("%issuer%", issuer == null ? "" : issuer)
                            .replace("%target%", target == null ? "" : target)
                            .replace("%amount%", String.valueOf(amount))
                            .replace("%expires%", finalExpires == null ? "" : finalExpires)
                    ))
                    .collect(Collectors.toList());

            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }

        ConfigurationSection controls = menuConfig.getConfigurationSection("menu.controls");
        if (controls != null) {
            for (String key : controls.getKeys(false)) {
                ConfigurationSection cs = controls.getConfigurationSection(key);
                int ctrlSlot = cs.getInt("slot");
                Material matCtrl = Material.valueOf(cs.getString("material", "ARROW").toUpperCase());
                ItemStack btn = new ItemStack(matCtrl);
                ItemMeta bm = btn.getItemMeta();
                bm.setDisplayName(Colors.color(cs.getString("name", key)));
                List<String> btnLore = cs.getStringList("lore").stream().map(Colors::color).collect(Collectors.toList());
                bm.setLore(btnLore);
                btn.setItemMeta(bm);
                inv.setItem(ctrlSlot, btn);
            }
        }

        ConfigurationSection custom = menuConfig.getConfigurationSection("menu.custom");
        if (custom != null) {
            for (String key : custom.getKeys(false)) {
                ConfigurationSection cs = custom.getConfigurationSection(key);
                int slot = cs.getInt("slot");
                Material matCustom = Material.valueOf(cs.getString("material", "STONE").toUpperCase());
                ItemStack it = new ItemStack(matCustom);
                if (matCustom == Material.PLAYER_HEAD) {
                    ItemMeta im = it.getItemMeta();
                    SkullMeta sm = (SkullMeta) im;
                    String owner = cs.getString("skull-owner", "");
                    if (owner != null && !owner.isEmpty()) {
                        OfflinePlayer off = Bukkit.getOfflinePlayer(owner);
                        sm.setOwningPlayer(off);
                    }
                    sm.setDisplayName(Colors.color(cs.getString("name", key)));
                    List<String> l = cs.getStringList("lore").stream().map(Colors::color).collect(Collectors.toList());
                    sm.setLore(l);
                    it.setItemMeta(sm);
                } else {
                    ItemMeta im = it.getItemMeta();
                    im.setDisplayName(Colors.color(cs.getString("name", key)));
                    List<String> l = cs.getStringList("lore").stream().map(Colors::color).collect(Collectors.toList());
                    im.setLore(l);
                    it.setItemMeta(im);
                }
                inv.setItem(slot, it);
            }
        }


        player.openInventory(inv);
    }
}
