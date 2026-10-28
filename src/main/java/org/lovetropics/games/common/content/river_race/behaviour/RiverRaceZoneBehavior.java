package org.lovetropics.games.common.content.river_race.behaviour;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.DyeColor;
import org.lovetropics.games.common.content.river_race.RiverRaceState;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public record RiverRaceZoneBehavior(
		String id,
		RegionQuery region,
		Component ordinalName,
		Component displayName,
		DyeColor color
) implements IGameBehavior {
	public static final MapCodec<RiverRaceZoneBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(RiverRaceZoneBehavior::id),
			RegionQuery.CODEC.fieldOf("region").forGetter(RiverRaceZoneBehavior::region),
			ComponentSerialization.CODEC.fieldOf("ordinal_name").forGetter(RiverRaceZoneBehavior::ordinalName),
			ComponentSerialization.CODEC.fieldOf("display_name").forGetter(RiverRaceZoneBehavior::displayName),
			DyeColor.CODEC.fieldOf("color").forGetter(RiverRaceZoneBehavior::color)
	).apply(i, RiverRaceZoneBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		RiverRaceState riverRace = game.state().get(RiverRaceState.KEY);
		riverRace.addZone(id, ordinalName, displayName, color, region.one(game));
	}
}
