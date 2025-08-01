package me.muszek_.playerBounty.listeners;

import me.muszek_.playerBounty.Colors;
import me.muszek_.playerBounty.PlayerBounty;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotifyListener implements Listener {

	private final PlayerBounty plugin;

	public UpdateNotifyListener(PlayerBounty plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (!player.isOp() && !player.hasPermission("*")) return;
		if (!plugin.isUpdateAvailable()) return;

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			player.sendMessage(Colors.color("&7[&2PlayerBounty&7] &fPlugin &2PlayerBounty&f has new update " +
					plugin.getLatestVersion() + "&f! Check:&b https://www.spigotmc.org/resources/124041/"));
		}, 60L);
	}
}
