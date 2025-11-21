package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.action.SetStatisticAction;
import com.lovetropics.minigames.common.core.game.player.PlayerIterable;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Optional;
import java.util.UUID;

public record StatisticNotifierBehavior(
		StatisticKey<Integer> statistic,
		boolean onlyIncrease,
		GameActionList actions,
		SetStatisticAction.Scope scope,
		Optional<IncreaseSound> increaseSound
) implements IGameBehavior {
	public static final MapCodec<StatisticNotifierBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(StatisticNotifierBehavior::statistic),
			Codec.BOOL.optionalFieldOf("only_increase", true).forGetter(StatisticNotifierBehavior::onlyIncrease),
			GameActionList.MAP_CODEC.forGetter(StatisticNotifierBehavior::actions),
			SetStatisticAction.Scope.CODEC.optionalFieldOf("scope", SetStatisticAction.Scope.PLAYER).forGetter(StatisticNotifierBehavior::scope),
			// TODO: Should really be its own action
			IncreaseSound.CODEC.optionalFieldOf("increase_sound").forGetter(StatisticNotifierBehavior::increaseSound)
	).apply(i, StatisticNotifierBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);
		switch (scope) {
			case PLAYER -> setupForPlayers(game, events);
			case TEAM -> setupForTeams(game, events);
			case GLOBAL -> setupGlobal(game, events);
		}
	}

	private void setupForPlayers(IGamePhase game, EventRegistrar events) {
		Object2IntMap<UUID> lastValues = new Object2IntOpenHashMap<>();

		events.listen(GamePlayerEvents.ADD, player ->
				// We don't want to notify players about statistics they already had when they joined
				lastValues.put(player.getUUID(), game.statistics().forPlayer(player).getInt(statistic))
		);
		events.listen(GamePlayerEvents.REMOVE, player -> lastValues.removeInt(player.getUUID()));

		events.listen(GamePhaseEvents.TICK, () -> {
			for (ServerPlayer player : game.allPlayers()) {
				int value = game.statistics().forPlayer(player).getInt(statistic);
				int lastValue = lastValues.put(player.getUUID(), value);
				if (lastValue != value) {
					onValueChange(game, value, lastValue, ActionSubjects.ofPlayer(player));
				}
			}
		});
	}

	private void setupForTeams(IGamePhase game, EventRegistrar events) {
		Object2IntMap<GameTeamKey> lastValues = new Object2IntOpenHashMap<>();

		events.listen(GamePhaseEvents.START, players -> {
			for (GameTeamKey team : game.statistics().getTeams()) {
				lastValues.put(team, game.statistics().forTeam(team).getInt(statistic));
			}
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			for (GameTeamKey team : game.statistics().getTeams()) {
				int value = game.statistics().forTeam(team).getInt(statistic);
				int lastValue = lastValues.put(team, value);
				if (lastValue != value) {
					onValueChange(game, value, lastValue, ActionSubjects.ofTeam(team));
				}
			}
		});
	}

	private void setupGlobal(IGamePhase game, EventRegistrar events) {
		MutableInt lastValue = new MutableInt();

		events.listen(GamePhaseEvents.START, players ->
				lastValue.setValue(game.statistics().global().getInt(statistic))
		);

		events.listen(GamePhaseEvents.TICK, () -> {
			int value = game.statistics().global().getInt(statistic);
			if (lastValue.getValue() != value) {
				onValueChange(game, value, lastValue.getValue(), ActionSubjects.EMPTY);
				lastValue.setValue(value);
			}
		});
	}

	private void onValueChange(IGamePhase game, int value, int lastValue, ActionSubjects<?> subjects) {
		if (onlyIncrease && value < lastValue) {
			return;
		}

		int increase = value - lastValue;
		ContextMap context = new ContextMap.Builder()
				.withParameter(GameActionContextKeys.CHANGE, increase)
				.create(ContextKeySet.EMPTY);
		actions.apply(game, context, subjects);

		if (increaseSound.isPresent()) {
			IncreaseSound config = increaseSound.get();
			for (int i = 0; i < increase; i += config.step()) {
				float pitch = Mth.lerp((float) i / increase, config.startPitch(), config.endPitch());
				game.scheduler().runAfterTicks(config.initialDelay() + i * config.interval(), () ->
						PlayerIterable.from(subjects.asPlayers(game)).playSound(config.sound().value(), SoundSource.NEUTRAL, 1.0f, pitch)
				);
			}
		}
	}

	private record IncreaseSound(
			Holder<SoundEvent> sound,
			int step,
			int initialDelay,
			int interval,
			float startPitch,
			float endPitch
	) {
		public static final Codec<IncreaseSound> CODEC = RecordCodecBuilder.create(i -> i.group(
				SoundEvent.CODEC.fieldOf("sound").forGetter(IncreaseSound::sound),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("step", 1).forGetter(IncreaseSound::step),
				ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("initial_delay", 5).forGetter(IncreaseSound::initialDelay),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("interval", 3).forGetter(IncreaseSound::interval),
				Codec.floatRange(0.2f, 5.0f).optionalFieldOf("start_pitch", 1.0f).forGetter(IncreaseSound::startPitch),
				Codec.floatRange(0.2f, 5.0f).optionalFieldOf("end_pitch", 2.0f).forGetter(IncreaseSound::endPitch)
		).apply(i, IncreaseSound::new));
	}
}
