package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.behavior.BehaviorList;
import com.lovetropics.minigames.common.core.game.map.IGameMapProvider;
import com.lovetropics.minigames.common.util.Util;
import net.minecraft.world.phys.AABB;

/**
 * For data driven game phase info
 */
public interface IGamePhaseDefinition {
	IGameMapProvider getMap();

	default AABB getGameArea() {
		return Util.INFINITE_AABB;
	}

	BehaviorList createBehaviors();
}
