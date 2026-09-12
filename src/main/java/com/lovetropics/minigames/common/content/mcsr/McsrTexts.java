package com.lovetropics.minigames.common.content.mcsr;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public final class McsrTexts {
	private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.mcsr.");

	public static final Component PREPARING_WORLD = KEYS.add("preparing_world", "Preparing your world...")
			.withStyle(ChatFormatting.GRAY);

	public static void collectTranslations(BiConsumer<String, String> consumer) {
		KEYS.forEach(consumer);
		consumer.accept(LoveTropics.ID + ".minigame.mcsr", "Speedrun");
	}
}
