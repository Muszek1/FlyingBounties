package me.muszek_.playerBounty;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colors {
	private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");


	public static String color(String input) {
		if (input == null) return "";

		Matcher matcher = HEX_PATTERN.matcher(input);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String hex = matcher.group(1);
			StringBuilder repl = new StringBuilder("§x");
			for (char c : hex.toCharArray()) {
				repl.append('§').append(c);
			}
			matcher.appendReplacement(sb, repl.toString());
		}
		matcher.appendTail(sb);

		return ChatColor.translateAlternateColorCodes('&', sb.toString());
	}
}
