package org.lovetropics.games.common.core.command;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;

public final class LoveTropicsEntityOptions {
	public static void register() {
		EntitySelectorOptions.register(
				"current_world",
				parser -> {
					StringReader reader = parser.getReader();
					boolean enable = reader.readBoolean();
					if (enable) {
						parser.setWorldLimited();
					}
				},
				parser -> true,
				Component.literal("Filters the search to only apply to the source world.")
		);
	}
}
