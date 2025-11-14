package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.behavior.BehaviorList;
import com.lovetropics.minigames.common.core.game.map.IGameMapProvider;

/**
 * For data driven game phase info
 */
public interface IGamePhaseDefinition {
	IGameMapProvider getMap();

	BehaviorList createBehaviors();
}
