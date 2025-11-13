package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbSelfState;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public final class BbClientStateBehavior implements IGameBehavior {
	public static final MapCodec<BbClientStateBehavior> CODEC = MapCodec.unit(BbClientStateBehavior::new);

	private final Object2ObjectMap<GameTeamKey, Currency> trackedCurrency = new Object2ObjectOpenHashMap<>();

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);

		events.listen(GamePlayerEvents.REMOVE, this::removePlayer);

		events.listen(BbEvents.CURRENCY_ACCUMULATE, (team, value, lastValue) -> {
			updateState(game, teams, team, currency -> currency.value = value);
		});

		events.listen(BbEvents.CURRENCY_INCREMENT_CHANGED, (team, value, lastValue) -> {
			updateState(game, teams, team, currency -> currency.nextIncrement = value);
		});
	}

	private void removePlayer(ServerPlayer player) {
		GameClientState.removeFromPlayer(BiodiversityBlitz.SELF_STATE.get(), player);
		GameClientState.removeFromPlayer(BiodiversityBlitz.MOB_SPAWN.get(), player);
	}

	private void updateState(IGamePhase game, TeamState teams, GameTeamKey team, Consumer<Currency> update) {
		Currency currency = trackedCurrency.computeIfAbsent(team, k -> new Currency());
		update.accept(currency);

		ClientBbSelfState state = new ClientBbSelfState(currency.value, currency.nextIncrement);
		for (ServerPlayer player : teams.getPlayersForTeam(game, team)) {
			GameClientState.sendToPlayer(state, player);
		}
	}

	static final class Currency {
		int value;
		int nextIncrement;

		@Override
		public String toString() {
			return "$(" + value + ", +" + nextIncrement + ")";
		}
	}
}
