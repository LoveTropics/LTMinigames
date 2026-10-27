package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public record PreventCropGrowthInRegionBehaviour(
		RegionQuery regions
) implements IGameBehavior {
	public static final MapCodec<PreventCropGrowthInRegionBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("regions").forGetter(c -> c.regions)
	).apply(i, PreventCropGrowthInRegionBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameWorldEvents.CROP_GROW, (level, blockPos) -> {
			if (regions.contains(game, level, blockPos)) {
				return TriState.FALSE;
			}
			return TriState.DEFAULT;
		});
	}
}
