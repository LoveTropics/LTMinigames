package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ClockTimeMarkers;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;

import java.util.Optional;

public record SetDayTimeNowBehavior(Optional<Long> time, Optional<ResourceKey<ClockTimeMarker>> timeMarker) implements IGameBehavior {
	public static final MapCodec<SetDayTimeNowBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.LONG.optionalFieldOf("time").forGetter(c -> c.time),
			ResourceKey.codec(ClockTimeMarkers.ROOT_ID).optionalFieldOf("time_marker").forGetter(o -> o.timeMarker)
	).apply(i, SetDayTimeNowBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			game.allLevels().forEach(level ->
					level.dimensionType().defaultClock().ifPresent(clock -> {
						time.ifPresent(aLong -> {
							level.clockManager().setTotalTicks(clock, aLong);
						});
						timeMarker.ifPresent(clockTimeMarkerResourceKey -> {
							level.clockManager().moveToTimeMarker(clock, clockTimeMarkerResourceKey);
						});
					}));
			return true;
		});
	}
}
