package org.lovetropics.games.common.content.turtle_race;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;
import org.lovetropics.games.common.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TurtleRaceTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.turtle_race.");

	public static final Component CHECKMARK = Component.literal("✔").withStyle(ChatFormatting.GREEN);

	public static final Component WARNING = KEYS.add("warning", "Warning!").withStyle(ChatFormatting.RED);
	public static final Component WRONG_WAY = KEYS.add("wrong_way", "You are going the wrong way!").withStyle(ChatFormatting.GOLD);
	public static final Component UNKNOWN_PLAYER = KEYS.add("unknown_player", "Unknown");
	public static final TranslationCollector.Fun2 LAP_COUNT = KEYS.add2("lap_count", "Lap #%s of %s").withStyle(ChatFormatting.AQUA);
	private static final TranslationCollector.Fun2 LAP_PROGRESS = KEYS.add2("lap_progress", "(Lap #%s; %s%%)").withStyle(ChatFormatting.GRAY);
	private static final TranslationCollector.Fun1 PROGRESS = KEYS.add1("progress", "(%s%%)").withStyle(ChatFormatting.GRAY);
	private static final TranslationCollector.Fun3 FINISHED_LAP = KEYS.add3("finished_lap", "%s finished lap #%s in %s!").withStyle(ChatFormatting.AQUA);
	private static final TranslationCollector.Fun2 FINISHED = KEYS.add2("finished", "%s finished in %s!").withStyle(ChatFormatting.AQUA);
	private static final TranslationCollector.Fun1 CLOSING = KEYS.add1("closing", "The game will finish in %s seconds!").withStyle(ChatFormatting.AQUA);

	static {
		KEYS.add("go", "GO!");
		KEYS.add("speed_boost", "Speed boost! You passed a checkpoint!");
	}

	private static MutableComponent formatTime(long totalSeconds) {
		return Component.literal(Util.formatMinutesSeconds(totalSeconds)).withStyle(ChatFormatting.GOLD);
	}

	public static Component progress(float progress) {
		return PROGRESS.apply(Math.round(progress * 100.0f));
	}

	public static Component lapProgress(int lap, float progress) {
		return LAP_PROGRESS.apply(lap + 1, Math.round(progress * 100.0f));
	}

	private static MutableComponent withCheckmark(Component text) {
		return Component.empty().append(CHECKMARK).append(" ").append(text);
	}

	public static Component finishedLap(Component playerName, int lap, long totalSeconds) {
		return withCheckmark(FINISHED_LAP.apply(playerName, lap + 1, formatTime(totalSeconds)));
	}

	public static Component finished(Component playerName, long totalSeconds) {
		return withCheckmark(FINISHED.apply(playerName, formatTime(totalSeconds)));
	}

	public static Component closing(int seconds) {
		return CLOSING.apply(Component.literal(String.valueOf(seconds)).withStyle(ChatFormatting.GOLD));
	}
}
