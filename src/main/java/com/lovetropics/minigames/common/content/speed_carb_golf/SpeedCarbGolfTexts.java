package com.lovetropics.minigames.common.content.speed_carb_golf;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.network.chat.MutableComponent;

public class SpeedCarbGolfTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.speed_carb_golf.");

	public static final MutableComponent ALL_HOLES_COMPLETE = KEYS.add("all_holes_complete", "All holes completed!");
	public static final MutableComponent HOLE_COMPLETE = KEYS.add("hole_complete", "Hole completed!");
	public static final MutableComponent MOVE_ON = KEYS.add("move_on", "Move on to next hole!");
	public static final MutableComponent YOUR_TURN = KEYS.add("your_turn", "It's your turn to play!");
	public static final TranslationCollector.Fun1 PLAYERS_TURN = KEYS.add1("players_turn", "It's %s's turn to play!");
}
