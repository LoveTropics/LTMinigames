package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.util.LinearSpline;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record SetStatisticAction(
		StatisticKey<Integer> statistic,
		int value,
		LinearSpline valueByPlayerCount,
		Scope scope
) implements IGameBehavior {
	public static final MapCodec<SetStatisticAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.typedCodec(Integer.class).fieldOf("statistic").forGetter(SetStatisticAction::statistic),
			Codec.INT.optionalFieldOf("value", 0).forGetter(SetStatisticAction::value),
			LinearSpline.CODEC.optionalFieldOf("by_player_count", LinearSpline.constant(0.0f)).forGetter(SetStatisticAction::valueByPlayerCount),
			Scope.CODEC.fieldOf("scope").forGetter(SetStatisticAction::scope)
	).apply(i, SetStatisticAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			int value = resolve(game);
			return scope.applyTo(game, targets, statistics -> statistics.set(statistic, value));
		});
	}

	private int resolve(IGamePhase game) {
		int valueByPlayerCount = Mth.floor(this.valueByPlayerCount.get(game.participants().size()));
		return Math.max(value, valueByPlayerCount);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SET_STATISTIC;
	}

	public enum Scope implements StringRepresentable {
		GLOBAL("global"),
		PLAYER("player"),
		TEAM("team"),
		;

		public static final Codec<Scope> CODEC = StringRepresentable.fromEnum(Scope::values);

		private final String name;

		Scope(String name) {
			this.name = name;
		}

		public boolean applyTo(IGamePhase game, ActionSubjects<?> targets, Consumer<StatisticsMap> consumer) {
			return switch (this) {
				case GLOBAL -> {
					consumer.accept(game.statistics().global());
					yield true;
				}
				case PLAYER -> {
					List<ServerPlayer> players = targets.asPlayers(game);
					for (ServerPlayer player : players) {
						consumer.accept(game.statistics().forPlayer(player));
					}
					yield !players.isEmpty();
				}
				case TEAM ->  {
					List<GameTeamKey> teams = targets.asTeams(game);
					for (GameTeamKey team : teams) {
						consumer.accept(game.statistics().forTeam(team));
					}
					yield !teams.isEmpty();
				}
			};
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
