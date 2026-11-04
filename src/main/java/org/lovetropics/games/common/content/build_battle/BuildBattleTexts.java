package org.lovetropics.games.common.content.build_battle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

public class BuildBattleTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.build_battle.");

	public static final Component BUILDING_START = KEYS.add("building_start", "Build-off!").withStyle(ChatFormatting.GREEN);
	public static final Component BUILDING_START_SUBTITLE = KEYS.add("building_start.subtitle", "Build a tropical masterpiece!").withStyle(ChatFormatting.YELLOW);
	public static final Component BUILDING_END = KEYS.add("building_end", "Building has ended!").withStyle(ChatFormatting.YELLOW);
	public static final Component REVIEW_TIME = KEYS.add("review_time", "Review time!").withStyle(ChatFormatting.GREEN);
	public static final Component REVIEW_TIME_SUBTITLE = KEYS.add("review_time.subtitle", "The jury will review your wonderful creations").withStyle(ChatFormatting.YELLOW);
	public static final TranslationCollector.Fun1 GIVE_POINTS = KEYS.add1("give_points", "You gave %d points!");

	public static final TranslationCollector.Fun1 COUNTDOWN = KEYS.add1("countdown", "%s remaining");
	public static final Component BAR_BUILDING = KEYS.add("bar.building_time", "Building time!");
	public static final TranslationCollector.Fun1 BAR_REVIEWING = KEYS.add1("bar.reviewing", "Reviewing %s...");

	public static final TranslationCollector.Fun1 ITEM_POINTS = KEYS.add1("item.points", "%d points");
	public static final Component ITEM_PREVIOUS = KEYS.add("item.previous", "Previous");
	public static final Component ITEM_NEXT = KEYS.add("item.next", "Next");
}
