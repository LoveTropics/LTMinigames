package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.tutorial;

import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;

// Players shouldn't be able to modify plots until the tutorial is done
public class TutorialState implements IGameState {
	public static final GameStateKey<TutorialState> KEY = GameStateKey.create("BioBlitz Tutorial Data");

	private boolean tutorialFinished = false;

	public void finishTutorial() {
		tutorialFinished = true;
	}

	public boolean isTutorialFinished() {
		return tutorialFinished;
	}
}
