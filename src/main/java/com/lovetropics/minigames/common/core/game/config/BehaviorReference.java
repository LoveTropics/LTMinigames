package com.lovetropics.minigames.common.core.game.config;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;

import java.util.function.BiConsumer;

public final class BehaviorReference {
	private final GameBehaviorType<?> type;
	private final Dynamic<?> config;

	public BehaviorReference(GameBehaviorType<?> type, Dynamic<?> config) {
		this.type = type;
		this.config = config;
	}

	public void addTo(BiConsumer<GameBehaviorType<?>, IGameBehavior> add) {
		DataResult<? extends IGameBehavior> result = type.codec.parse(config);

		result.result().ifPresent(behavior -> add.accept(type, behavior));

		result.error().ifPresent(error -> {
			LoveTropics.LOGGER.warn("Failed to parse behavior declaration of type {}: {}", type, error);
		});
	}
}
