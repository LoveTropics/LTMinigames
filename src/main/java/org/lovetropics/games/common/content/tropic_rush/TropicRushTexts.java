package org.lovetropics.games.common.content.tropic_rush;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

import java.util.function.BiConsumer;

public final class TropicRushTexts {
	private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.tropic_rush.");

	public static final Component PREPARING_WORLD = KEYS.add("preparing_world", "Preparing your world...")
			.withStyle(ChatFormatting.GRAY);
	public static final TranslationCollector.Fun1 KILLED_DRAGON = KEYS.add1("killed_dragon", "%s killed the Ender Dragon and finished the run!");
	public static final TranslationCollector.Fun1 COMPLETED_BOARD = KEYS.add1("completed_board", "%s completed the whole bingo card and finished the run!");
	public static final Component TIME_UP = KEYS.add("time_up", "Time's up!")
			.withStyle(ChatFormatting.GOLD);
	public static final TranslationCollector.Fun3 RESULT = KEYS.add3("result", "%s. %s - %s points");
	public static final TranslationCollector.Fun3 RESULT_FINISHED = KEYS.add3("result_finished", "%s. %s - finished with %s points");

	// Spying
	public static final Component SPYING_PICK_TARGET = KEYS.add("spying.pick_target", "Spy on...");
	public static final TranslationCollector.Fun1 SPYING_PICK_ACTION = KEYS.add1("spying.pick_action", "Spy on %s by...");
	public static final Component SPYING_SEE_INVENTORY = KEYS.add("spying.see_inventory", "See their inventory");
	public static final Component SPYING_WATCH = KEYS.add("spying.watch", "Watch through their eyes");
	public static final TranslationCollector.Fun1 SPYING_INVENTORY = KEYS.add1("spying.inventory", "Inventory of %s");
	public static final TranslationCollector.Fun1 SPYING_WATCHING = KEYS.add1("spying.watching", "Watching %s - sneak to return");
	public static final TranslationCollector.Fun1 SPYING_BUSY = KEYS.add1("spying.busy", "%s is already spying on someone right now");

	// Modifiers
	public static final Component RULES = KEYS.add("rules", "Rules");
	public static final TranslationCollector.Fun1 OPEN_RULES_HINT = KEYS.add1("rules.open_hint", "[%s] Rules");
	public static final Component RULES_EMPTY = KEYS.add("rules.empty", "No special rules in this game");
	public static final Component RULE_UNKNOWN = KEYS.add("rules.unknown", "???");
	public static final TranslationCollector.Fun2 RULE_LEVEL = KEYS.add2("rules.level", "Level %s/%s");
	public static final TranslationCollector.Fun3 MODIFIER_STRONGER = KEYS.add3("modifier.stronger", "%s grew stronger! (level %s/%s)");
	public static final TranslationCollector.Fun3 MODIFIER_WEAKER = KEYS.add3("modifier.weaker", "%s grew weaker (level %s/%s)");
	public static final Component MYSTERY_STRONGER = KEYS.add("modifier.mystery_stronger", "A mysterious rule grew stronger...");
	public static final Component MYSTERY_WEAKER = KEYS.add("modifier.mystery_weaker", "A mysterious rule grew weaker...");
	public static final TranslationCollector.Fun2 MODIFIER_REVEALED = KEYS.add2("modifier.revealed", "You figured out a rule: %s - %s");
	public static final TranslationCollector.Fun1 UNKNOWN_MODIFIER = KEYS.add1("modifier.unknown", "No modifier with id: %s");
	public static final TranslationCollector.Fun3 MODIFIER_LEVEL = KEYS.add3("modifier.level", "%s: level %s/%s");
	public static final TranslationCollector.Fun1 MODIFIER_REVEALED_TO = KEYS.add1("modifier.revealed_to", "Revealed a rule to %s players");
	public static final TranslationCollector.Fun1 MODIFIER_HIDDEN_FROM = KEYS.add1("modifier.hidden_from", "Hid a rule from %s players");

	// Overlord
	public static final Component OVERLORD = KEYS.add("overlord", "Overlord");
	public static final TranslationCollector.Fun1 OPEN_OVERLORD_HINT = KEYS.add1("overlord.open_hint", "[%s] Overlord");
	public static final Component OVERLORD_NO_PLAYERS = KEYS.add("overlord.no_players", "Nobody is playing");

	public static void collectTranslations(BiConsumer<String, String> consumer) {
		KEYS.forEach(consumer);
		consumer.accept(LoveTropics.ID + ".minigame.tropic_rush", "Tropic Rush");
	}
}
