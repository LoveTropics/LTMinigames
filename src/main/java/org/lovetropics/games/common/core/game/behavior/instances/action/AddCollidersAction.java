package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.ColliderState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.function.Supplier;

public record AddCollidersAction(List<String> regions) implements IGameBehavior {
	public static final MapCodec<AddCollidersAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions").forGetter(AddCollidersAction::regions)
	).apply(i, AddCollidersAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		ColliderState colliders = ColliderState.getOrAdd(game, events);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			for (String region : regions) {
				BlockBox collider = game.mapRegions().getOrThrow(region);
				colliders.addCollider(region, collider);
			}
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ADD_COLLIDERS;
	}
}
