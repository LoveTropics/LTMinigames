package com.lovetropics.minigames.common.content.drr_showdown;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.network.chat.MutableComponent;

public class DDRShowdownTexts {

	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.ddr_showdown.");

	public static final MutableComponent NAME = KEYS.add("name", "DDR Showdown");
	public static final MutableComponent SCORES = KEYS.add("scores", "Scores");
	public static final MutableComponent POINTS = KEYS.add("score", "%s Points");
	public static final MutableComponent SIDE_BAR = KEYS.add("sidebar", "Points Leaderboard");
	public static final MutableComponent WINNER = KEYS.add("winner", "Won the DDR Showdown!");
}
