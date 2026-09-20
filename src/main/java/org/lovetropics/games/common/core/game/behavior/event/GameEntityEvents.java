package org.lovetropics.games.common.core.game.behavior.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;

public final class GameEntityEvents {
	public static final GameEventType<Mounted> MOUNTED = GameEventType.create(Mounted.class, listeners -> (level, entityMounting, entityBeingMounted) -> {
		for (Mounted listener : listeners) {
			TriState result = listener.onEntityMounted(level, entityMounting, entityBeingMounted);
			if (!result.isDefault()) {
				return result;
			}
		}
		return TriState.DEFAULT;
	});

	private GameEntityEvents() {
	}

	public interface Mounted {
		TriState onEntityMounted(ServerLevel level, Entity entityMounting, Entity entityBeingMounted);
	}
}
