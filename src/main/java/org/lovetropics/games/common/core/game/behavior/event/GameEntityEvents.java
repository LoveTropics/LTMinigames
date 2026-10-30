package org.lovetropics.games.common.core.game.behavior.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public final class GameEntityEvents {
	public static final GameEventType<Mounting> MOUNT = GameEventType.create(Mounting.class, listeners -> (level, entityMounting, entityBeingMounted) -> {
		for (Mounting listener : listeners) {
			TriState result = listener.onEntityMount(level, entityMounting, entityBeingMounted);
			if (!result.isDefault()) {
				return result;
			}
		}
		return TriState.DEFAULT;
	});
	public static final GameEventType<Mounting> DISMOUNT = GameEventType.create(Mounting.class, listeners -> (level, entityMounting, entityBeingMounted) -> {
		for (Mounting listener : listeners) {
			TriState result = listener.onEntityMount(level, entityMounting, entityBeingMounted);
			if (!result.isDefault()) {
				return result;
			}
		}
		return TriState.DEFAULT;
	});

	private GameEntityEvents() {
	}

	public interface Mounting {
		TriState onEntityMount(ServerLevel level, Entity entityMounting, @Nullable Entity entityBeingMounted);
	}
}
