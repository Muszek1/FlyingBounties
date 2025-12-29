package me.muszek_.playerBounty.listeners;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import me.muszek_.playerBounty.menusystem.BountyMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

import java.io.File;

public class BountyMenuListener implements Listener {
    private final PlayerBounty plugin;
    private final BountyMenu menu;

    private Component baseTitleComponent;
    private String baseTitleString;

    private YamlConfiguration menuConfig;

    public BountyMenuListener(PlayerBounty plugin) {
        this.plugin = plugin;
        this.menu = new BountyMenu(plugin);
        reloadMenuConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reloadMenuConfig() {
        File file = new File(plugin.getDataFolder(), "menu.yml");
        if (!file.exists()) plugin.saveResource("menu.yml", false);
        this.menuConfig = YamlConfiguration.loadConfiguration(file);

        String rawTitle = menuConfig.getString("menu.title", "Bounties");
        this.baseTitleComponent = Colors.color(rawTitle);

        this.baseTitleString = LegacyComponentSerializer.legacySection().serialize(this.baseTitleComponent);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(baseTitleString)) return;

        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        Inventory clicked = e.getClickedInventory();

        if (clicked != null && !clicked.equals(e.getView().getTopInventory())) {
            if (!e.isShiftClick()) {
                e.setCancelled(false);
                return;
            }
        }

        int slot = e.getRawSlot();
        ConfigurationSection controls = menuConfig.getConfigurationSection("menu.controls");
        if (controls == null) return;

        for (String key : controls.getKeys(false)) {
            ConfigurationSection cs = controls.getConfigurationSection(key);
            if (cs == null || cs.getInt("slot") != slot) continue;

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
        if (e.getView().getTitle().startsWith(baseTitleString)) {
            Inventory top = e.getView().getTopInventory();
            for (int slot : e.getRawSlots()) {
                if (slot < top.getSize()) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent e) {
        boolean isMyMenu = e.getDestination().getViewers().stream()
            .anyMatch(viewer -> viewer.getOpenInventory().getTitle().startsWith(baseTitleString));

        if (isMyMenu) {
            e.setCancelled(true);
        }
    }

    private int extractPage(String title) {
        try {
            int openBracket = title.lastIndexOf('[');
            if (openBracket == -1) {
                openBracket = title.lastIndexOf('(');
            }

            int slash = title.lastIndexOf('/');

            if (openBracket != -1 && slash != -1 && slash > openBracket) {
                String numStr = title.substring(openBracket + 1, slash);
                return Integer.parseInt(numStr.replaceAll("§.", "").trim()) - 1;
            }
        } catch (Exception ex) {
        }
        return 0;
    }
}