package com.lovetropics.minigames.common.content.paint_party;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.network.chat.MutableComponent;

public class PaintPartyTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.paint_party.");
	public static final MutableComponent COCONUT_POWER_UP = KEYS.add("coconut_power_up", "Exploding Coconut");
	public static final MutableComponent COCONUT_POWER_UP_SUBTITLE = KEYS.add("coconut_power_up.subtitle", "Throw these coconuts around!");
	public static final MutableComponent SPEED_POWER_UP = KEYS.add("speed_power_up", "Speed Power Up");
	public static final MutableComponent SPEED_POWER_UP_SUBTITLE = KEYS.add("speed_power_up.subtitle", "Speed mode engaged!");
}
