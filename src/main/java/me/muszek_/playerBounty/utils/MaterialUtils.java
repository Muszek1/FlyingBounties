package me.muszek_.playerBounty.utils;

import org.bukkit.Material;

public class MaterialUtils {
    public static Material getPlayerHead() {
        try {
            return Material.valueOf("PLAYER_HEAD");
        } catch (IllegalArgumentException e) {
            return Material.valueOf("SKULL_ITEM");
        }
    }
}
