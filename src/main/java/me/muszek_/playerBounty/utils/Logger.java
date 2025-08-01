package me.muszek_.playerBounty.utils;

import me.muszek_.playerBounty.Colors;
import org.bukkit.Bukkit;


public class Logger {
	public static void log(LogLevel level, String message) {
		if (message == null) return;

		switch (level) {
			case ERROR:
				Bukkit.getConsoleSender().sendMessage(Colors.color("&8[&c&lERROR&r&8] &f" + message));
				break;
			case WARNING:
				Bukkit.getConsoleSender().sendMessage(Colors.color("&8[&6&lWARNING&r&8] &f" + message));
				break;
			case INFO:
				Bukkit.getConsoleSender().sendMessage(Colors.color("&8[&e&lINFO&r&8] &f" + message));
				break;
			case SUCCESS:
				Bukkit.getConsoleSender().sendMessage(Colors.color("&8[&a&lSUCCESS&r&8] &f" + message));
				break;
			case OUTLINE:
				Bukkit.getConsoleSender().sendMessage(Colors.color("&7" + message));
				break;
		}
	}

	public enum LogLevel {ERROR, WARNING, INFO, SUCCESS, OUTLINE}
}