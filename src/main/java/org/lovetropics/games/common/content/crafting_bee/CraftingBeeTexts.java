package org.lovetropics.games.common.content.crafting_bee;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class CraftingBeeTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.crafting_bee.");

	public static final TranslationCollector.Fun3 TEAM_HAS_COMPLETED_RECIPES = KEYS.add3("team_has_completed_recipes", "Team %s has completed %s out of %s recipes");
	public static final Component DONT_CHEAT = KEYS.add("dont_cheat", "Don't cheat!").withStyle(ChatFormatting.RED);
	public static final Component HINT = KEYS.add("hint", "Click to show a hint, displaying the position of a random number of ingredients");
	public static final TranslationCollector.Fun1 HINTS_LEFT = KEYS.add1("hints_left", "You have %s hints left");
	public static final Component CANNOT_RECYCLE = KEYS.add("cannot_recycle", "You can only recycle items that you have crafted!").withStyle(ChatFormatting.RED);
	public static final Component TIME_UP = KEYS.add("time_up", "Time's up!").withStyle(ChatFormatting.GOLD);
	public static final TranslationCollector.Fun2 NOT_ENOUGH_TO_RECYCLE = KEYS.add2("not_enough_to_recycle", "You need at least %s %s to recycle this").withStyle(ChatFormatting.RED);
	public static final TranslationCollector.Fun1 TIME_PENALTY = KEYS.add1("time_penalty", "-%s seconds").withStyle(ChatFormatting.RED);
}
