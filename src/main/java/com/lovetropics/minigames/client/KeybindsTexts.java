package com.lovetropics.minigames.client;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;

import java.util.function.BiConsumer;

public final class KeybindsTexts {
	public static final TranslationCollector KEYS = new TranslationCollector("key." + LoveTropics.ID + ".");

	public static void collectTranslations(BiConsumer<String, String> consumer) {
		KEYS.add("expand_bingo_board", "Expand Bingo Board");
		KEYS.add("join", "Join");
		KEYS.add("leave", "Leave");
		KEYS.add("manage", "Manage");
		KEYS.forEach(consumer);
		consumer.accept("key.category." + LoveTropics.ID + ".lovetropics", "LoveTropics");
	}
}
