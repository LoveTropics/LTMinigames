package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.BiConsumer;

public class EscapeRaceTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.escape_race.");

	public static final Component TERRY_TRASH = KEYS.add("terry_trash", "Terry's Trash").withStyle(ChatFormatting.GOLD);

	public static final List<Component> DDR_POSITIVE_PHRASES = List.of(
			KEYS.add("ddr.positive.nice_one", "Nice One!"),
			KEYS.add("ddr.positive.close_enough", "Close Enough!"),
			KEYS.add("ddr.positive.sick", "Sick Work!"),
			KEYS.add("ddr.positive.smashedit", "Smashed It!"),
			KEYS.add("ddr.positive.poppingoff", "Popping Off!"),
			KEYS.add("ddr.positive.incredible", "Incredible!")
	);

	public static final Component DDR_PERFECT_SCORE = KEYS.add("ddr.positive.perfect", "Perfect!");
	public static final Component DDR_STREAK_BROKEN = KEYS.add("ddr.negative.streak_broken", "Streak Broken!").withStyle(ChatFormatting.RED);
	public static final TranslationCollector.Fun2 DDR_SCORE_ADDED = KEYS.add2("ddr.score.added", "%s earned %s Break Bucks from DDR");
	public static final TranslationCollector.Fun2 SPENT_BREAK_BUCKS = KEYS.add2("spent_break_bucks", "%s spent %s Break Bucks");
	public static final TranslationCollector.Fun1 GIVEN_BREAK_BUCKS = KEYS.add1("given_break_bucks", "You have been given %s Break Bucks");

	public static final TranslationCollector.Fun1 UNLOCKING = KEYS.add1("room.unlocking", "UNLOCKING - %s%%");
	public static final TranslationCollector.Fun2 LOCKED_CANNOT_AFFORD = KEYS.add2("room.locked_cannot_afford", "LOCKED - %s/%s Break Bucks");
	public static final TranslationCollector.Fun2 LOCKED_NOT_ENOUGH_PLAYERS = KEYS.add2("room.locked_not_enough_players", "LOCKED - %s/%s crouched");

	public static final TranslationCollector.Fun1 BREAK_BUCKS_COST = KEYS.add1("vending_machine.break_bucks_cost", "%s Break Bucks");

	public static final Component SIDEBAR_VACATION_DAYS = KEYS.add("sidebar.vacation_days", "Vacation Days").withStyle(ChatFormatting.GOLD);
	public static final TranslationCollector.Fun1 VACATION_DAYS_CHANGED = KEYS.add1("race.vacation_days_changed", "+%s Vacation Day(s)")
			.withStyle(ChatFormatting.GREEN);
	public static final TranslationCollector.Fun4 SIDEBAR_HEADER = KEYS.add4("sidebar.vacation_days_header", "%s %s | %s %s").withStyle(ChatFormatting.GRAY);
	public static final Component VACATION_DAY = Component.literal("\u0355").withStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("lt", "ui")));


	public static void collectTranslations(BiConsumer<String, String> consumer){
		KEYS.add("donorbook.title", "%s's final thoughts");

		KEYS.forEach(consumer);
		consumer.accept(LoveTropics.ID + ".minigame.escape_race", "Escape Race");
	}
}
