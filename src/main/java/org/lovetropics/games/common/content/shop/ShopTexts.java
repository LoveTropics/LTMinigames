package org.lovetropics.games.common.content.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

public final class ShopTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.shop.");

	public static final Component SHOP = KEYS.add("title", "Shop");
	public static final Component BONUSES = KEYS.add("bonuses", "Bonuses");
	public static final Component SABOTAGES = KEYS.add("sabotages", "Sabotages");
	public static final Component BONUS = KEYS.add("bonus", "Bonus").withStyle(ChatFormatting.GREEN);
	public static final Component SABOTAGE = KEYS.add("sabotage", "Sabotage").withStyle(ChatFormatting.RED);
	public static final TranslationCollector.Fun1 TROPICOINS = KEYS.add1("tropicoins", "%s Tropicoins");
	public static final TranslationCollector.Fun1 OPEN_SHOP_HINT = KEYS.add1("open_hint", "[%s] Shop");
	public static final Component NOT_ENOUGH_TROPICOINS = KEYS.add("not_enough_tropicoins", "Not enough Tropicoins");
	public static final TranslationCollector.Fun1 CANNOT_AFFORD = KEYS.add1("cannot_afford", "You can't afford %s yet!");
	public static final TranslationCollector.Fun1 PURCHASE_FAILED = KEYS.add1("purchase_failed", "%s can't be bought right now");
	public static final TranslationCollector.Fun1 SOLD_OUT_FOR_YOU = KEYS.add1("sold_out_for_you", "You can't buy any more %s!");
	public static final Component SOLD_OUT = KEYS.add("sold_out", "Sold out");
	public static final TranslationCollector.Fun1 STOCK_LEFT = KEYS.add1("stock_left", "%s left");
	public static final TranslationCollector.Fun1 TARGET_COOLDOWN = KEYS.add1("target_cooldown", "Can't be used on the same player again for %s");
	public static final TranslationCollector.Fun2 TARGET_ON_COOLDOWN = KEYS.add2("target_on_cooldown", "%s can't get %s again yet!");
	public static final Component ALL_ON_COOLDOWN = KEYS.add("all_on_cooldown", "Everyone got this too recently, come back later!");
	public static final TranslationCollector.Fun1 DONATION_RATE = KEYS.add1("donation_rate", "Donations give %s Tropicoins per unit donated");

	// Used on self
	public static final TranslationCollector.Fun1 USED = KEYS.add1("used", "You used %s!").withStyle(ChatFormatting.GREEN);
	public static final TranslationCollector.Fun2 USED_WITH_COST = KEYS.add2("used_with_cost", "You bought %s for %s Tropicoins!").withStyle(ChatFormatting.GREEN);

	// Use on other target
	public static final TranslationCollector.Fun2 USED_ON = KEYS.add2("used_on_free", "You used %s on %s!").withStyle(ChatFormatting.GREEN);
	public static final TranslationCollector.Fun3 USED_ON_WITH_COST = KEYS.add3("used_on", "You used %s on %s for %s Tropicoins!").withStyle(ChatFormatting.GREEN);
	public static final TranslationCollector.Fun2 BONUS_FROM = KEYS.add2("bonus_from", "%s gave you %s!").withStyle(ChatFormatting.GREEN);
	public static final TranslationCollector.Fun2 SABOTAGED_BY = KEYS.add2("sabotaged_by", "%s sabotaged you with %s!").withStyle(ChatFormatting.RED);
	public static final TranslationCollector.Fun3 USE_ON_BROADCAST = KEYS.add3("use_on_broadcast", "%s used %s on %s!");

	// Use global
	public static final TranslationCollector.Fun2 USE_GLOBAL_BROADCAST = KEYS.add2("use_global_broadcast", "%s triggered %s!");

	public static final Component NO_TARGETS = KEYS.add("no_targets", "There is nobody to use this on");
	public static final Component TARGET_UNAVAILABLE = KEYS.add("target_unavailable", "That player can't be targeted right now");
	public static final Component TARGET_PICK = KEYS.add("target_pick", "Pick a target").withStyle(ChatFormatting.GRAY);
	public static final Component TARGET_RANDOM = KEYS.add("target_random", "Hits a target randomly").withStyle(ChatFormatting.GRAY);
	public static final TranslationCollector.Fun1 PICK_TARGET = KEYS.add1("pick_target", "Use %s on...");
	public static final Component PICK_RANDOM = KEYS.add("pick_random", "Pick randomly");
	public static final Component PICK_CANCEL = KEYS.add("pick_cancel", "Cancel");
	public static final TranslationCollector.Fun1 POINTS = KEYS.add1("points", "%s points");
}
