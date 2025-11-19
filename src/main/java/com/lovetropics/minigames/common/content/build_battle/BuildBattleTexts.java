package com.lovetropics.minigames.common.content.build_battle;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.network.chat.Component;

public class BuildBattleTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.build_battle.");

	public static final TranslationCollector.Fun2 POINTS_DISPLAY = KEYS.add2("results.points_display", "%s - %d points");
	public static final Component RESULTS = KEYS.add("results", "Results:");
	public static final Component BUILDING_END = KEYS.add("building_end", "Building phase has ended!");
	public static final Component REVIEW_TIME = KEYS.add("review_time", "Time for the jury to review your wonderful creations...");
	public static final TranslationCollector.Fun1 GIVE_POINTS = KEYS.add1("give_points", "You gave %d points!");

	public static final TranslationCollector.Fun2 COUNTDOWN = KEYS.add2("countdown", "%d:%d remaining");
	public static final Component BAR_BUILDING = KEYS.add("bar.building_time", "Building time!");
	public static final TranslationCollector.Fun1 BAR_REVIEWING = KEYS.add1("bar.reviewing", "Reviewing %s...");

	public static final TranslationCollector.Fun1 ITEM_POINTS = KEYS.add1("item.points", "%d points");
	public static final Component ITEM_PREVIOUS = KEYS.add("item.previous", "Previous");
	public static final Component ITEM_NEXT = KEYS.add("item.next", "Next");
}
