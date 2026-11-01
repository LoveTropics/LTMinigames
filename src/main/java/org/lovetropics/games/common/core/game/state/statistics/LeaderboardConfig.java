package org.lovetropics.games.common.core.game.state.statistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.Optional;
import java.util.function.Consumer;

public record LeaderboardConfig<T extends Comparable<T>>(
		StatisticKey<T> statistic,
		PlacementOrder order,
		int length,
		boolean showPlacement,
		boolean showScore,
		Optional<Holder> holder
) {
	public static final MapCodec<LeaderboardConfig<?>> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.CODEC.fieldOf("statistic").forGetter(c -> c.statistic),
			PlacementOrder.CODEC.optionalFieldOf("order", PlacementOrder.MAX).forGetter(c -> c.order),
			Codec.INT.optionalFieldOf("length", 5).forGetter(c -> c.length),
			Codec.BOOL.optionalFieldOf("show_placement", true).forGetter(c -> c.showPlacement),
			Codec.BOOL.optionalFieldOf("show_score", true).forGetter(c -> c.showScore),
			Holder.CODEC.optionalFieldOf("statistic_holder").forGetter(c -> c.holder)
	).apply(i, LeaderboardConfig::createUnchecked));

	@SuppressWarnings("unchecked")
	private static <T extends Comparable<T>> LeaderboardConfig<T> createUnchecked(StatisticKey<?> statistic, PlacementOrder order, int length, boolean showPlacement, boolean showScore, Optional<Holder> holder) {
		return new LeaderboardConfig<>((StatisticKey<T>) statistic, order, length, showPlacement, showScore, holder);
	}

	public void buildDisplay(IGamePhase game, Consumer<Component> output) {
		Placement.Score<?, ?> placement;
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (teams == null || (holder.isPresent() && holder.get() == Holder.PLAYER)) {
			placement = Placement.fromPlayerScore(order, game, statistic);
		} else {
			placement = Placement.fromTeamScore(order, game, statistic);
		}
		placement.buildDisplay(length, showPlacement, showScore, output);
	}

	public enum Holder implements StringRepresentable {
		PLAYER("player"),
		TEAM("team"),
		;

		public static final Codec<Holder> CODEC = StringRepresentable.fromEnum(Holder::values);

		private final String id;

		Holder(String id) {
			this.id = id;
		}

		@Override
		public String getSerializedName() {
			return id;
		}
	}
}
