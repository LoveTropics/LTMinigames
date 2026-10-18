package org.lovetropics.games.common.content.mcsr;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

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

	// Shop
	public static final Component SHOP = KEYS.add("shop", "Shop");
	public static final TranslationCollector.Fun1 TROPICOINS = KEYS.add1("shop.tropicoins", "%s tropicoins");
	public static final TranslationCollector.Fun1 OPEN_SHOP_HINT = KEYS.add1("shop.open_hint", "[%s] Shop");
	public static final Component NOT_ENOUGH_TROPICOINS = KEYS.add("shop.not_enough_tropicoins", "Not enough tropicoins");
	public static final TranslationCollector.Fun2 PURCHASED = KEYS.add2("shop.purchased", "You bought %s for %s tropicoins!");
	public static final TranslationCollector.Fun1 CANNOT_AFFORD = KEYS.add1("shop.cannot_afford", "You can't afford %s yet!");
	public static final TranslationCollector.Fun1 PURCHASE_FAILED = KEYS.add1("shop.purchase_failed", "%s can't be bought right now");

	// Shop - sabotage
	public static final TranslationCollector.Fun3 USED_ON = KEYS.add3("shop.used_on", "You used %s on %s for %s tropicoins!");
	public static final TranslationCollector.Fun2 SABOTAGED_BY = KEYS.add2("shop.sabotaged_by", "%s sabotaged you with %s!");
	public static final TranslationCollector.Fun3 SABOTAGE_BROADCAST = KEYS.add3("shop.sabotage_broadcast", "%s used %s on %s!");
	public static final Component NO_TARGETS = KEYS.add("shop.no_targets", "There is nobody to use this on");
	public static final Component TARGET_UNAVAILABLE = KEYS.add("shop.target_unavailable", "That player can't be targeted right now");
	public static final Component SABOTAGE_PICK = KEYS.add("shop.sabotage_pick", "Sabotage: pick a player");
	public static final Component SABOTAGE_RANDOM = KEYS.add("shop.sabotage_random", "Sabotage: hits a random player");
	public static final TranslationCollector.Fun1 PICK_TARGET = KEYS.add1("shop.pick_target", "Use %s on...");
	public static final Component PICK_RANDOM = KEYS.add("shop.pick_random", "Random player");
	public static final Component PICK_CANCEL = KEYS.add("shop.pick_cancel", "Cancel");
	public static final TranslationCollector.Fun1 POINTS = KEYS.add1("shop.points", "%s points");

	public static void collectTranslations(BiConsumer<String, String> consumer) {
		KEYS.forEach(consumer);
		consumer.accept(LoveTropics.ID + ".minigame.mcsr", "Speedrun");
	}
}
