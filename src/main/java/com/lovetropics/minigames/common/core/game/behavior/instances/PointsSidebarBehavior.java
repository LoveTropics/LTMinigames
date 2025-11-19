package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.Placement;
import com.lovetropics.minigames.common.core.game.state.statistics.PlacementOrder;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameSidebar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PointsSidebarBehavior(
		StatisticKey<Integer> statistic,
		PlacementOrder order,
		int count,
		Component title,
		List<TemplatedText> header,
		Optional<Holder> holder
) implements IGameBehavior {
	public static final MapCodec<PointsSidebarBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(PointsSidebarBehavior::statistic),
			PlacementOrder.CODEC.optionalFieldOf("order", PlacementOrder.MAX).forGetter(PointsSidebarBehavior::order),
			Codec.INT.optionalFieldOf("count", 5).forGetter(PointsSidebarBehavior::count),
			ComponentSerialization.CODEC.fieldOf("title").forGetter(PointsSidebarBehavior::title),
			TemplatedText.CODEC.listOf().fieldOf("header").forGetter(PointsSidebarBehavior::header),
			Holder.CODEC.optionalFieldOf("statistic_holder").forGetter(PointsSidebarBehavior::holder)
	).apply(i, PointsSidebarBehavior::new));

	private static final int REFRESH_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		final GameSidebar sidebar = GameWidgets.getOrRegister(game, events).openGlobalSidebar(title);
		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % REFRESH_INTERVAL == 0) {
				sidebar.set(renderSidebar(game));
			}
		});
	}

	private Component[] renderSidebar(final IGamePhase game) {
		int totalCount = 0;
		for (final PlayerKey player : game.statistics().getPlayers()) {
			totalCount += game.statistics().forPlayer(player).getInt(statistic);
		}
		for (final GameTeamKey team : game.statistics().getTeams()) {
			totalCount += game.statistics().forTeam(team).getInt(statistic);
		}

		final List<Component> sidebar = new ArrayList<>(10);
		for (final TemplatedText line : header) {
			sidebar.add(line.apply(Map.of("total", Component.literal(String.valueOf(totalCount)))));
		}

		final Placement.Score<?, Integer> placement;
		final TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (teams == null || (holder.isPresent() && holder.get() == Holder.PLAYER)) {
			placement = Placement.fromPlayerScore(order, game, statistic, false);
		} else {
			placement = Placement.fromTeamScore(order, game, statistic);
		}
		placement.addToSidebar(sidebar, count);

		return sidebar.toArray(new Component[0]);
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
