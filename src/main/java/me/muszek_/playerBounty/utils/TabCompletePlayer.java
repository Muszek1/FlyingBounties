package me.muszek_.playerBounty.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TabCompletePlayer {

	private TabCompletePlayer() {
	}

	public static List<String> getOnlinePlayerNames() {
		return Bukkit.getServer().getOnlinePlayers().stream()
				.map(Player::getName)
				.collect(Collectors.toList());
	}
}
