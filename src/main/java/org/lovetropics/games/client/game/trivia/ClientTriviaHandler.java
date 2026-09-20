package org.lovetropics.games.client.game.trivia;

import org.lovetropics.games.common.core.network.trivia.ShowTriviaMessage;
import org.lovetropics.games.common.core.network.trivia.TriviaAnswerResponseMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class ClientTriviaHandler {

	public static void showScreen(ShowTriviaMessage message) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreenAndShow(new TriviaQuestionScreen(message.triviaBlock(), message.question(), message.triviaBlockState()));
	}

	public static void handleResponse(TriviaAnswerResponseMessage message) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui.screen() instanceof TriviaQuestionScreen triviaQuestionScreen) {
			triviaQuestionScreen.handleAnswerResponse(message.triviaBlockState());
		}
	}
}
