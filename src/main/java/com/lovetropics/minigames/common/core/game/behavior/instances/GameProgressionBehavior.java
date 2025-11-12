package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommand;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPoint;
import com.lovetropics.minigames.common.util.LinearSpline;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.SharedConstants;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Map;

public class GameProgressionBehavior implements IGameBehavior {
	public static final MapCodec<GameProgressionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(b -> b.channel),
			Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("named_points", Map.of()).forGetter(b -> b.namedPoints),
			Codec.INT.optionalFieldOf("max_time_step", 1).forGetter(b -> b.maxTimeStep),
			ExtraCodecs.compactListCodec(ProgressionPeriod.CODEC).optionalFieldOf("fixed_time_step", List.of()).forGetter(b -> b.fixedTimeStep),
			PlayerConstraint.CODEC.listOf().optionalFieldOf("time_by_player_count", List.of()).forGetter(b -> b.playerConstraints),
			Codec.BOOL.optionalFieldOf("start", true).forGetter(b -> b.start)
	).apply(i, GameProgressionBehavior::new));

	private ProgressHolder progressHolder;

	private final ProgressChannel channel;
	private final Map<String, Float> namedPoints;
	private final int maxTimeStep;
	private final List<ProgressionPeriod> fixedTimeStep;
	private final List<PlayerConstraint> playerConstraints;
	private final boolean start;

	private Float2FloatFunction playerCountToTime = key -> 0.0f;

	private int debugTimeMultiplier = 1;

	public GameProgressionBehavior(ProgressChannel channel, Map<String, Float> namedPoints, int maxTimeStep, List<ProgressionPeriod> fixedTimeStep, List<PlayerConstraint> playerConstraints, boolean start) {
		this.channel = channel;
		this.namedPoints = namedPoints;
		this.maxTimeStep = maxTimeStep;
		this.fixedTimeStep = fixedTimeStep;
		this.playerConstraints = playerConstraints;
		this.start = start;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		progressHolder = channel.registerTo(game);
		namedPoints.forEach((name, value) -> progressHolder.addNamedPoint(name, Math.round(value * SharedConstants.TICKS_PER_SECOND)));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, () -> {
			if (!playerConstraints.isEmpty()) {
				playerCountToTime = resolvePlayerConstraints(playerConstraints, game.participants().size(), progressHolder);
			}
			if (start) {
				progressHolder.start();
			}
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (progressHolder.isPaused()) {
				return;
			}
			int newTime = tickTime(game, progressHolder.time());
			progressHolder.set(newTime);
		});

		game.controlCommands().add("pause", ControlCommand.forAdmins(source -> debugTimeMultiplier = 0));
		game.controlCommands().add("resume", ControlCommand.forAdmins(source -> debugTimeMultiplier = 1));
		game.controlCommands().add("fastForward", ControlCommand.forAdmins(source -> debugTimeMultiplier *= 2));
	}

	private int tickTime(IGamePhase game, int time) {
		if (progressHolder.is(fixedTimeStep)) {
			return time + debugTimeMultiplier;
		}

		int playerCount = game.participants().size();

		int targetTimeByPlayerCount = Mth.ceil(playerCountToTime.get(playerCount));
		if (targetTimeByPlayerCount > time) {
			int step = Math.min(targetTimeByPlayerCount - time, maxTimeStep);
			return time + step * debugTimeMultiplier;
		} else {
			return time + debugTimeMultiplier;
		}
	}

	private static Float2FloatFunction resolvePlayerConstraints(List<PlayerConstraint> constraints, int initialPlayerCount, ProgressHolder progression) {
		LinearSpline.Builder spline = LinearSpline.builder();
		for (PlayerConstraint constraint : constraints) {
			spline.point(constraint.count().resolve(initialPlayerCount), constraint.point().resolve(progression));
		}
		return spline.build();
	}

	private record PlayerConstraint(PlayerCount count, ProgressionPoint point) {
		public static final Codec<PlayerConstraint> CODEC = RecordCodecBuilder.create(i -> i.group(
				PlayerCount.CODEC.forGetter(PlayerConstraint::count),
				ProgressionPoint.CODEC.fieldOf("point").forGetter(PlayerConstraint::point)
		).apply(i, PlayerConstraint::new));
	}

	private record PlayerCount(float percentage, int left, int killed) {
		private static final float NO_PERCENTAGE = -1.0f;
		private static final int NO_COUNT = -1;

		public static final MapCodec<PlayerCount> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.FLOAT.optionalFieldOf("percentage", NO_PERCENTAGE).forGetter(PlayerCount::percentage),
				Codec.INT.optionalFieldOf("left", NO_COUNT).forGetter(PlayerCount::left),
				Codec.INT.optionalFieldOf("killed", NO_COUNT).forGetter(PlayerCount::killed)
		).apply(i, PlayerCount::new));

		public int resolve(int initialCount) {
			int value = 0;
			if (percentage != NO_PERCENTAGE) {
				value = Math.max(value, Mth.floor(percentage * initialCount));
			}
			if (left != NO_COUNT) {
				value = Math.max(value, left);
			}
			if (killed != NO_COUNT) {
				value = Math.max(value, initialCount - killed);
			}
			return value;
		}
	}
}
