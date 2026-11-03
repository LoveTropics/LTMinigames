package org.lovetropics.games.common.core.game.persistent.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;

import java.util.List;

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
	public MapCodec<CompositePersistentBehavior> codec() {
		return MAP_CODEC;
	}
}
