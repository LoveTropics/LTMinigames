package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.HidePlayersState;
import org.lovetropics.games.common.core.game.player.PlayerRole;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record HidePlayersBehavior(
		Map<PlayerRole, List<GameType>> gameModesToHideByRole
) implements IGameBehavior {
	public static final MapCodec<HidePlayersBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(PlayerRole.CODEC, GameType.CODEC.listOf()).fieldOf("game_modes_to_hide_by_role").forGetter(HidePlayersBehavior::gameModesToHideByRole)
	).apply(i, HidePlayersBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Map<PlayerRole, IntSet> hiddenPlayersByRole = new EnumMap<>(PlayerRole.class);
		for (PlayerRole role : PlayerRole.values()) {
			hiddenPlayersByRole.put(role, new IntOpenHashSet());
		}

		events.listen(GamePhaseEvents.TICK, () -> {
			for (Map.Entry<PlayerRole, IntSet> entry : hiddenPlayersByRole.entrySet()) {
				IntSet hiddenPlayerIds = entry.getValue();
				List<GameType> gameModesToHide = gameModesToHideByRole.getOrDefault(entry.getKey(), List.of());
				boolean changedRole = false;
				for (ServerPlayer player : game.allPlayers()) {
					GameType gameMode = player.gameMode();
					if (gameModesToHide.contains(gameMode)) {
						changedRole |= hiddenPlayerIds.add(player.getId());
					} else {
						changedRole |= hiddenPlayerIds.remove(player.getId());
					}
				}
				if (changedRole) {
					HidePlayersState hiddenPlayers = new HidePlayersState(new IntOpenHashSet(hiddenPlayerIds));
					GameClientState.sendToPlayers(hiddenPlayers, game.getPlayersWithRole(entry.getKey()));
				}
			}
		});

		events.listen(GamePlayerEvents.REMOVE, player -> {
			for (Map.Entry<PlayerRole, IntSet> entry : hiddenPlayersByRole.entrySet()) {
				IntSet hiddenPlayerIds = entry.getValue();
				if (hiddenPlayerIds.remove(player.getId())) {
					HidePlayersState hiddenPlayers = new HidePlayersState(new IntOpenHashSet(hiddenPlayerIds));
					GameClientState.sendToPlayers(hiddenPlayers, game.getPlayersWithRole(entry.getKey()));
				}
			}
			GameClientState.removeFromPlayer(GameClientStateTypes.HIDE_PLAYERS.get(), player);
		});

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			HidePlayersState hiddenPlayers = new HidePlayersState(new IntOpenHashSet(hiddenPlayersByRole.getOrDefault(role, IntSet.of())));
			GameClientState.sendToPlayer(hiddenPlayers, player);
		});
	}
}
