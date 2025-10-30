package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class EscapeRaceTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.escape_race.");

	public static final Component TERRY_TRASH = KEYS.add("terry_trash", "Terry's Trash").withStyle(ChatFormatting.GOLD);
}
