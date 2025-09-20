package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public final class CampingTrackerBehavior implements IGameBehavior {
	public static final MapCodec<CampingTrackerBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TriggerAfterConfig.CODEC.optionalFieldOf("trigger", TriggerAfterConfig.EMPTY).forGetter(c -> c.trigger),
			Codec.LONG.optionalFieldOf("camp_time_threshold", 20L * 8).forGetter(c -> c.campTimeThreshold),
			Codec.DOUBLE.optionalFieldOf("camp_movement_threshold", 8.0).forGetter(c -> c.campMovementThreshold)
	).apply(i, CampingTrackerBehavior::new));

	private static final long CAMP_TEST_INTERVAL = 20;

	private final TriggerAfterConfig trigger;
	private final long campTimeThreshold;
	private final double campMovementThreshold;

	private final Map<UUID, CampingTracker> campingTrackers = new Object2ObjectOpenHashMap<>();

	public CampingTrackerBehavior(TriggerAfterConfig trigger, long campTimeThreshold, double campMovementThreshold) {
		this.trigger = trigger;
		this.campTimeThreshold = campTimeThreshold;
		this.campMovementThreshold = campMovementThreshold;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		trigger.awaitThen(game, events, () -> {
			events.listen(GamePhaseEvents.TICK, () -> tick(game));
			events.listen(GamePlayerEvents.REMOVE, this::onPlayerLeave);
			events.listen(GamePlayerEvents.DEATH, this::onPlayerDeath);
		});
	}

	private void tick(IGamePhase game) {
		long ticks = game.ticks();
		if (ticks % CAMP_TEST_INTERVAL == 0) {
			testForCamping(game, ticks);
		}
	}

	private void testForCamping(IGamePhase game, long time) {
		GameStatistics statistics = game.statistics();

		for (ServerPlayer player : game.participants()) {
			CampingTracker tracker = getCampingTracker(player);

			Vec3 currentPosition = player.position();
			if (tracker.camping) {
				int campingTime = tracker.trackCamping(currentPosition, time);
				if (campingTime > 0) {
					statistics.forPlayer(player).withDefault(StatisticKey.TIME_CAMPING, () -> 0)
							.apply(current -> current + campingTime);
				}
			} else {
				tracker.trackNotCamping(currentPosition, time);
			}
		}
	}

	private CampingTracker getCampingTracker(ServerPlayer player) {
		return campingTrackers.computeIfAbsent(player.getUUID(), i -> new CampingTracker());
	}

	private TriState onPlayerDeath(ServerPlayer player, DamageSource source) {
		campingTrackers.remove(player.getUUID());
		return TriState.DEFAULT;
	}

	private void onPlayerLeave(ServerPlayer player) {
		campingTrackers.remove(player.getUUID());
	}

	class CampingTracker {
		boolean camping;

		@Nullable
		Vec3 lastPosition;
		long lastTrackTime;

		void trackNotCamping(Vec3 currentPosition, long time) {
			if (lastPosition == null) {
				lastPosition = currentPosition;
				lastTrackTime = time;
			}

			if (time - lastTrackTime > campTimeThreshold) {
				double movement = currentPosition.distanceTo(lastPosition);

				lastTrackTime = time;
				lastPosition = currentPosition;

				if (movement < campMovementThreshold) {
					camping = true;
				}
			}
		}

		int trackCamping(Vec3 currentPosition, long time) {
			double movement = lastPosition != null ? currentPosition.distanceTo(lastPosition) : Double.MAX_VALUE;
			if (movement > campMovementThreshold) {
				camping = false;
				lastPosition = currentPosition;
			}

			long duration = time - lastTrackTime;
			if (duration >= 20) {
				long seconds = duration / 20;
				lastTrackTime += seconds * 20;
				return (int) seconds;
			}

			return 0;
		}
	}
}
