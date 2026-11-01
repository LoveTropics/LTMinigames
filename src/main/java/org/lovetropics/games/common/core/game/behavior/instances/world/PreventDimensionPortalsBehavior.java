package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public record PreventDimensionPortalsBehavior(
		boolean nether,
		boolean theEnd
) implements IGameBehavior {

	public static final MapCodec<PreventDimensionPortalsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("nether", true).forGetter(PreventDimensionPortalsBehavior::nether),
			Codec.BOOL.optionalFieldOf("the_end", true).forGetter(PreventDimensionPortalsBehavior::theEnd)
	).apply(i, PreventDimensionPortalsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameWorldEvents.NETHER_PORTAL_SPAWN, (level, pos) -> nether);
		events.listen(GameWorldEvents.END_PORTAL_SPAWN, (level, pos) -> theEnd);
	}
}
