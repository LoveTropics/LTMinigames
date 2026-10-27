package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.ColliderState;

import java.util.function.Supplier;

public record AddCollidersAction(RegionQuery regions) implements IGameBehavior {
	public static final MapCodec<AddCollidersAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("regions").forGetter(AddCollidersAction::regions)
	).apply(i, AddCollidersAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		ColliderState colliders = ColliderState.getOrAdd(game, events);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			colliders.addColliders(regions.all(game));
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ADD_COLLIDERS;
	}
}
