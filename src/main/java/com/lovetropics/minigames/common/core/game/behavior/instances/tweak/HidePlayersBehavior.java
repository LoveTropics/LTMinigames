package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.HidePlayersState;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record HidePlayersBehavior(
		Map<PlayerRole, List<PlayerRole>> rolesToHideByRole
) implements IGameBehavior {
	public static final MapCodec<HidePlayersBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(PlayerRole.CODEC, PlayerRole.CODEC.listOf()).fieldOf("roles_to_hide_by_role").forGetter(HidePlayersBehavior::rolesToHideByRole)
	).apply(i, HidePlayersBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Map<PlayerRole, IntSet> hiddenPlayersByRole = new EnumMap<>(PlayerRole.class);
		for (PlayerRole role : PlayerRole.values()) {
			hiddenPlayersByRole.put(role, new IntOpenHashSet());
		}

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) ->
				updateRole(game, player, role, lastRole, hiddenPlayersByRole)
		);
		events.listen(GamePlayerEvents.REMOVE, player -> {
				updateRole(game, player, null, game.getRoleFor(player), hiddenPlayersByRole);
				GameClientState.removeFromPlayer(GameClientStateTypes.HIDE_PLAYERS.get(), player);
		});
	}

	private void updateRole(IGamePhase game, ServerPlayer player, @Nullable PlayerRole role, @Nullable PlayerRole lastRole, Map<PlayerRole, IntSet> hiddenPlayersByRole) {
		for (Map.Entry<PlayerRole, List<PlayerRole>> entry : rolesToHideByRole.entrySet()) {
			List<PlayerRole> rolesToHide = entry.getValue();
			IntSet hiddenPlayers = hiddenPlayersByRole.get(entry.getKey());
			if (lastRole != null && rolesToHide.contains(lastRole)) {
				hiddenPlayers.remove(player.getId());
			}
			if (role != null && rolesToHide.contains(role)) {
				hiddenPlayers.add(player.getId());
			}
		}

		for (ServerPlayer otherPlayer : game.allPlayers()) {
			PlayerRole otherRole = game.getRoleFor(player);
			IntSet hiddenPlayers = otherRole == null ? IntSet.of() : new IntOpenHashSet(hiddenPlayersByRole.get(otherRole));
			GameClientState.sendToPlayer(new HidePlayersState(hiddenPlayers), otherPlayer);
		}
	}
}
