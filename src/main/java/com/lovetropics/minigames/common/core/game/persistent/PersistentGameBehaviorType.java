package com.lovetropics.minigames.common.core.game.persistent;

import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public record PersistentGameBehaviorType<T extends PersistentGameBehavior>(MapCodec<T> codec) {
	@Override
	public String toString() {
		Identifier key = PersistentGameBehaviors.REGISTRY.getKey(this);
		return key != null ? key.toString() : "[unregistered]";
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		PersistentGameBehaviorType<?> that = (PersistentGameBehaviorType<?>) object;
		return codec == that.codec;
	}

	@Override
	public int hashCode() {
		return Objects.hash(codec);
	}
}
