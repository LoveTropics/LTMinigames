package org.lovetropics.games.common.content.hoedown;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

public final class HoedownTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.tropical_hoedown.");

	public static final Component WINNER_DROPPING_HATS = KEYS.add("winner_dropping_hats", "You won the last game, and are losing hats! Sorry!").withStyle(ChatFormatting.RED);
	public static final Component A_GENEROUS_DONOR = KEYS.add("a_generous_donor", "A generous donor");
	public static final Component ADDED_HAT = KEYS.add("added_hat", "Your head feels heavier. You have picked up a hat.").withStyle(ChatFormatting.GREEN);
}
