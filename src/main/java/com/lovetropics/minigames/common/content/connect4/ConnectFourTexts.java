package com.lovetropics.minigames.common.content.connect4;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ConnectFourTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.connect_four.");

	public static final Component IT_IS_YOUR_TURN_TITLE = KEYS.add("your_turn.title", "Your turn!").withStyle(ChatFormatting.GOLD);
	public static final Component IT_IS_YOUR_TURN_SUBTITLE = KEYS.add("your_turn.subtitle", "Place a block in the board").withStyle(ChatFormatting.GRAY);

	public static final TranslationCollector.Fun1 TEAM_GOES_NEXT = KEYS.add1("teams_goes_next", "Team %s goes next");
}
