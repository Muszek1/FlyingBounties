package me.muszek_.playerBounty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colors {

	private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

	public static Component color(String text) {
		if (text == null) return Component.empty();

		Matcher matcher = HEX_PATTERN.matcher(text);
		StringBuilder sb = new StringBuilder();

		while (matcher.find()) {
			String hex = matcher.group(1);
			StringBuilder repl = new StringBuilder("§x");
			for (char c : hex.toCharArray()) {
				repl.append('§').append(c);
			}
			matcher.appendReplacement(sb, repl.toString());
		}
		matcher.appendTail(sb);

		String legacyText = sb.toString().replace('&', '§');

		return LegacyComponentSerializer.legacySection().deserialize(legacyText);
	}

	public static Component color(String text, String... replacements) {
		if (text == null) return Component.empty();

		for (int i = 0; i < replacements.length; i += 2) {
			if (i + 1 >= replacements.length) {
				break;
			}

			String target = replacements[i];
			String replacement = replacements[i + 1];

			if (target != null && replacement != null) {
				text = text.replace(target, replacement);
			}
		}
		return color(text);
	}
}