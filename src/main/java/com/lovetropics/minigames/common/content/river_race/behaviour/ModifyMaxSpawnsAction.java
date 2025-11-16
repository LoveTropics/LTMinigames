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
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.util.Mth;

public record ModifyMaxSpawnsAction(
		float factor,
		int durationTicks
) implements IGameBehavior {
	public static final MapCodec<ModifyMaxSpawnsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.FLOAT.fieldOf("factor").forGetter(ModifyMaxSpawnsAction::factor),
			Codec.INT.fieldOf("duration_ticks").forGetter(ModifyMaxSpawnsAction::durationTicks)
	).apply(i, ModifyMaxSpawnsAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		RiverRaceState riverRace = game.state().get(RiverRaceState.KEY);

		Object2LongMap<GameTeamKey> expiryTimes = new Object2LongOpenHashMap<>();
		expiryTimes.defaultReturnValue(game.ticks());

		events.applyToTeams(game, (context, team) -> {
			expiryTimes.compute(team, (t, lastExpiryTime) -> {
				long baseTime = lastExpiryTime != null ? lastExpiryTime : game.ticks();
				return baseTime + durationTicks;
			});
			return true;
		});

		events.listen(RiverRaceEvents.MODIFY_MAX_SPAWN_COUNT, (pos, count) -> {
			GameTeamKey team = riverRace.getTeamAt(pos);
			if (team == null) {
				return count;
			}
			if (game.ticks() < expiryTimes.getLong(team)) {
				return Mth.ceil(count * factor);
			}
			return count;
		});
	}
}
