package com.lovetropics.minigames.common.core.game.behavior.event;

import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;

public final class GameEntityEvents {
	public static final GameEventType<Mounted> MOUNTED = GameEventType.create(Mounted.class, listeners -> (entityMounting, entityBeingMounted) -> {
		for (Mounted listener : listeners) {
			TriState result = listener.onEntityMounted(entityMounting, entityBeingMounted);
			if (!result.isDefault()) {
				return result;
			}
		}
		return TriState.DEFAULT;
	});

	private GameEntityEvents() {
	}

	public interface Mounted {
		TriState onEntityMounted(Entity entityMounting, Entity entityBeingMounted);
	}
}
