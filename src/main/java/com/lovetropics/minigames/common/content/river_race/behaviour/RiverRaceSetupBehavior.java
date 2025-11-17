package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.minigames.common.content.river_race.RiverRaceState;
import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;

import java.util.Map;

public record RiverRaceSetupBehavior(
		Direction forwardDirection,
		Map<GameTeamKey, String> teamRegions
) implements IGameBehavior {
	public static final MapCodec<RiverRaceSetupBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Direction.CODEC.fieldOf("forward_direction").forGetter(RiverRaceSetupBehavior::forwardDirection),
			Codec.unboundedMap(GameTeamKey.CODEC, Codec.STRING).fieldOf("team_regions").forGetter(RiverRaceSetupBehavior::teamRegions)
	).apply(i, RiverRaceSetupBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		RiverRaceState riverRace = game.state().get(RiverRaceState.KEY);
		riverRace.setForwardDirection(forwardDirection);

		teamRegions.forEach((team, regionKey) ->
				riverRace.setTeamRegion(team, game.mapRegions().getOrThrow(regionKey))
		);

		events.listen(RiverRaceEvents.UNLOCK_ZONE, id ->
				riverRace.setCurrentZone(riverRace.getZoneById(id))
		);
	}
}
