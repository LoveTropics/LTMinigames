package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;

import java.util.Optional;

public record PreventTramplingCropsBehaviour(
		Optional<RegionQuery> regions
) implements IGameBehavior {

	public static final MapCodec<PreventTramplingCropsBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.optionalFieldOf("regions").forGetter(c -> c.regions)
	).apply(i, PreventTramplingCropsBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameLivingEntityEvents.FARMLAND_TRAMPLE, (level, entity, blockPos, blockState) -> {
			if (regions.isEmpty() || regions.get().contains(game, level, blockPos)) {
				return TriState.FALSE;
			}
			return TriState.DEFAULT;
		});
	}
}
