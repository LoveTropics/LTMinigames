package org.lovetropics.games.common.content.shop;

import net.minecraft.network.chat.Component;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

public final class ShopTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.shop.");

	public static final Component SHOP = KEYS.add("title", "Shop");
	public static final TranslationCollector.Fun1 TROPICOINS = KEYS.add1("tropicoins", "%s tropicoins");
	public static final TranslationCollector.Fun1 OPEN_SHOP_HINT = KEYS.add1("open_hint", "[%s] Shop");
	public static final Component NOT_ENOUGH_TROPICOINS = KEYS.add("not_enough_tropicoins", "Not enough tropicoins");
	public static final TranslationCollector.Fun2 PURCHASED = KEYS.add2("purchased", "You bought %s for %s tropicoins!");
	public static final TranslationCollector.Fun1 CANNOT_AFFORD = KEYS.add1("cannot_afford", "You can't afford %s yet!");
	public static final TranslationCollector.Fun1 PURCHASE_FAILED = KEYS.add1("purchase_failed", "%s can't be bought right now");

	// Sabotage
	public static final TranslationCollector.Fun3 USED_ON = KEYS.add3("used_on", "You used %s on %s for %s tropicoins!");
	public static final TranslationCollector.Fun2 SABOTAGED_BY = KEYS.add2("sabotaged_by", "%s sabotaged you with %s!");
	public static final TranslationCollector.Fun3 SABOTAGE_BROADCAST = KEYS.add3("sabotage_broadcast", "%s used %s on %s!");
	public static final Component NO_TARGETS = KEYS.add("no_targets", "There is nobody to use this on");
	public static final Component TARGET_UNAVAILABLE = KEYS.add("target_unavailable", "That player can't be targeted right now");
	public static final Component SABOTAGE_PICK = KEYS.add("sabotage_pick", "Sabotage: pick a player");
	public static final Component SABOTAGE_RANDOM = KEYS.add("sabotage_random", "Sabotage: hits a random player");
	public static final TranslationCollector.Fun1 PICK_TARGET = KEYS.add1("pick_target", "Use %s on...");
	public static final Component PICK_RANDOM = KEYS.add("pick_random", "Random player");
	public static final Component PICK_CANCEL = KEYS.add("pick_cancel", "Cancel");
	public static final TranslationCollector.Fun1 POINTS = KEYS.add1("points", "%s points");
}
