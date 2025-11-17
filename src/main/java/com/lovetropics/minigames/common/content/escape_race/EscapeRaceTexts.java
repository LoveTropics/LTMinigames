package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public class EscapeRaceTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.escape_race.");

	public static final Component TERRY_TRASH = KEYS.add("terry_trash", "Terry's Trash").withStyle(ChatFormatting.GOLD);

	public static void collectTranslations(BiConsumer<String, String> consumer){
		KEYS.add("ddr.positive.nice_one", "Nice One!");
		KEYS.add("ddr.positive.close_enough", "Close Enough!");
		KEYS.add("ddr.positive.sick", "Sick Work!");
		KEYS.add("ddr.positive.smashedit", "Smashed It!");
		KEYS.add("ddr.positive.poppingoff", "Popping Off!");
		KEYS.add("ddr.positive.incredible", "Incredible!");
		KEYS.add("ddr.positive.perfect", "Perfect!");
		KEYS.add("ddr.negative.streak_broken", "Streak Broken!");
		KEYS.add("ddr.score.added", "+%s Break Bucks");
		KEYS.add("donorbook.title", "%s's final thoughts");

		KEYS.forEach(consumer);
		consumer.accept(LoveTropics.ID + ".minigame.escape_race", "Escape Race");
	}
}
