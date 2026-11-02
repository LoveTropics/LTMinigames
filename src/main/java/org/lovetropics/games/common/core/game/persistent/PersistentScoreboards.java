package org.lovetropics.games.common.core.game.persistent;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.Objective;

import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/// Probably can improve this at some point in the future, just for now having a "standard" way to define objective names
public class PersistentScoreboards {

	public static Objective getOrCreateObjective(ServerLevel serverLevel, String game, String id, String... additional) {
		StringBuilder pathBuilder = new StringBuilder("world_game.")
				.append(game)
				.append(".")
				.append(id);
		for (String add : additional) {
			pathBuilder.append(".").append(add);
		}
		String path = pathBuilder.toString();
		return getOrCreate(serverLevel, path);
	}

	private static Objective getOrCreate(ServerLevel level, String objectiveName) {
		Objective objective = level.getScoreboard().getObjective(objectiveName);
		if (objective == null) {
			objective = level.getScoreboard().addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER, true, null);
		}
		return objective;
	}
}
