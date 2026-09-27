package org.lovetropics.games.common.content.mcsr;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;

public final class McsrTexts {
	private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.mcsr.");

	public static final Component PREPARING_WORLD = KEYS.add("preparing_world", "Preparing your world...")
			.withStyle(ChatFormatting.GRAY);
	public static final TranslationCollector.Fun1 KILLED_DRAGON = KEYS.add1("killed_dragon", "%s killed the Ender Dragon and finished the run!");
	public static final TranslationCollector.Fun1 COMPLETED_BOARD = KEYS.add1("completed_board", "%s completed the whole bingo card and finished the run!");
	public static final Component TIME_UP = KEYS.add("time_up", "Time's up! Nobody finished the run, so the most points wins.")
			.withStyle(ChatFormatting.GOLD);
	public static final TranslationCollector.Fun3 RESULT = KEYS.add3("result", "%s. %s - %s points");
	public static final TranslationCollector.Fun3 RESULT_FINISHED = KEYS.add3("result_finished", "%s. %s - finished with %s points");

	public static void collectTranslations(BiConsumer<String, String> consumer) {
		KEYS.forEach(consumer);
		consumer.accept(LoveTropics.ID + ".minigame.mcsr", "Speedrun");
	}
}
