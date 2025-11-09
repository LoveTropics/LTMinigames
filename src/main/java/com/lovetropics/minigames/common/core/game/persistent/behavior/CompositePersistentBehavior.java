package com.lovetropics.minigames.common.core.game.persistent.behavior;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.List;
import java.util.function.Supplier;

public record CompositePersistentBehavior(List<PersistentGameBehavior> behaviors) implements PersistentGameBehavior {
	public static final Codec<CompositePersistentBehavior> CODEC = PersistentGameBehavior.CODEC.listOf().xmap(CompositePersistentBehavior::new, CompositePersistentBehavior::behaviors);
	public static final MapCodec<CompositePersistentBehavior> MAP_CODEC = CODEC.fieldOf("behaviors");

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		for (PersistentGameBehavior behavior : behaviors) {
			behavior.register(game, events);
		}
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.COMPOSITE;
	}
}
