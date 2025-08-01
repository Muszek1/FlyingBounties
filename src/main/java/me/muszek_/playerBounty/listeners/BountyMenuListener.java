package me.muszek_.playerBounty.listeners;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.menusystem.BountyMenu;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;

public class BountyMenuListener implements Listener {
    private final PlayerBounty plugin;
    private final BountyMenu menu;
    private String baseTitle;
    private YamlConfiguration menuConfig;

    public BountyMenuListener(PlayerBounty plugin) {
        this.plugin = plugin;
        this.menu = new BountyMenu(plugin);
        reloadMenuConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void reloadMenuConfig() {
        File file = new File(plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) plugin.saveResource("menu.yml", false);
        this.menuConfig = YamlConfiguration.loadConfiguration(file);
        this.baseTitle = Colors.color(menuConfig.getString("menu.title", "Zlecenia"));
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getView().getTitle().startsWith(baseTitle)) {
            reloadMenuConfig();
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(baseTitle)) return;

        Inventory clicked = e.getClickedInventory();
        Inventory top = e.getView().getTopInventory();

        if (clicked != null && clicked.equals(top)
                || e.isShiftClick() && e.getView().getBottomInventory().equals(clicked)) {
            e.setCancelled(true);
        }

        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        ConfigurationSection controls = menuConfig.getConfigurationSection("menu.controls");
        if (controls == null) return;

        for (String key : controls.getKeys(false)) {
            ConfigurationSection cs = controls.getConfigurationSection(key);
            if (cs.getInt("slot") != slot) continue;
            switch (key.toLowerCase()) {
                case "prev":
                    int prev = extractPage(title) - 1;
                    if (prev >= 0) menu.open(player, prev);
                    break;
                case "next":
                    int next = extractPage(title) + 1;
                    menu.open(player, next);
                    break;
                case "close":
                    player.closeInventory();
                    break;
            }
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!e.getView().getTitle().startsWith(baseTitle)) return;
        Inventory top = e.getView().getTopInventory();
        for (int slot : e.getRawSlots()) {
            if (slot < top.getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        String title = e.getDestination().getViewers().stream()
                .findFirst().map(v -> e.getDestination().getHolder() != null ? "" : baseTitle)
                .orElse("");
        if (title.startsWith(baseTitle)) {
            e.setCancelled(true);
        }
    }

    private int extractPage(String title) {
        try {
            int a = title.indexOf('('), b = title.indexOf('/');
            return Integer.parseInt(title.substring(a + 1, b)) - 1;
        } catch (Exception ex) {
            return 0;
        }
    }
}
