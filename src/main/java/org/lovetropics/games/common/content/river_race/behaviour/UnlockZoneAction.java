package org.lovetropics.games.common.content.river_race.behaviour;

import org.lovetropics.games.common.content.river_race.RiverRace;
import org.lovetropics.games.common.content.river_race.event.RiverRaceEvents;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Supplier;

public record UnlockZoneAction(
		String zone
) implements IGameBehavior {
	public static final MapCodec<UnlockZoneAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("zone").forGetter(UnlockZoneAction::zone)
	).apply(i, UnlockZoneAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			game.invoker(RiverRaceEvents.UNLOCK_ZONE).onUnlockZone(zone);
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return RiverRace.UNLOCK_ZONE_ACTION;
	}
}
