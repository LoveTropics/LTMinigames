package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameTeamEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerStorage;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record JoinLateWithRoleBehavior(PlayerRole role, RejoinMode rejoin) implements IGameBehavior {
	public static final MapCodec<JoinLateWithRoleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlayerRole.CODEC.fieldOf("role").forGetter(c -> c.role),
			RejoinMode.CODEC.optionalFieldOf("rejoin", RejoinMode.NONE).forGetter(c -> c.rejoin)
	).apply(i, JoinLateWithRoleBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		Set<PlayerKey> oldParticipants = new HashSet<>();
		Map<PlayerKey, GameTeamKey> oldTeams = new HashMap<>();

		events.listen(GamePlayerEvents.SELECT_ROLE_ON_JOIN, (player, requestedRole) -> {
			// Let the player be a spectator if they really want to
			if (requestedRole == PlayerRole.SPECTATOR) {
				return PlayerRole.SPECTATOR;
			}
			if (rejoin != RejoinMode.NONE && oldParticipants.remove(player)) {
				return PlayerRole.PARTICIPANT;
			}
			return role;
		});

		if (rejoin != RejoinMode.NONE) {
			events.listen(GamePlayerEvents.JOIN, player -> {
				PlayerKey playerKey = PlayerKey.from(player);
				GameTeamKey oldTeam = oldTeams.get(playerKey);
				if (teams != null && oldTeam != null) {
					teams.addPlayerTo(playerKey, oldTeam);
				}
			});

			events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
				PlayerKey playerKey = PlayerKey.from(player);
				if (role == PlayerRole.PARTICIPANT) {
					oldParticipants.add(playerKey);
				} else if (lastRole == PlayerRole.PARTICIPANT) {
					oldParticipants.remove(playerKey);
				}
			});

			events.listen(GameTeamEvents.SET_GAME_TEAM, (player, _, team) ->
					oldTeams.put(PlayerKey.from(player), team.key())
			);
			events.listen(GameTeamEvents.REMOVE_FROM_TEAM, (player, _, team) -> {
				// If the player is leaving, this is the normal clearing process - ignore
				if (!game.allPlayers().contains(player)) {
					return;
				}
				oldTeams.remove(PlayerKey.from(player), team.key());
			});

			// TODO: We would ideally have much more clearly defined flow for a player that rejoins - e.g. a game with death should have the player effectively die
			PlayerStorage playerStorage = new PlayerStorage();
			events.listen(GamePlayerEvents.REMOVE, player -> {
				if (rejoin == RejoinMode.RESTORE && game.getRoleFor(player) == PlayerRole.PARTICIPANT) {
					playerStorage.store(player);
				}
			});

			events.listen(GamePlayerEvents.LOAD, (player, role) -> {
				if (rejoin == RejoinMode.RESTORE && role == PlayerRole.PARTICIPANT) {
					return playerStorage.takePlayerData(player.id()).orElse(null);
				}
				return null;
			});
		}
	}

	public enum RejoinMode implements StringRepresentable {
		NONE("none"),
		RESTORE("restore"),
		RESPAWN("respawn"),
		;

		public static final Codec<RejoinMode> CODEC = StringRepresentable.fromEnum(RejoinMode::values);

		private final String name;

		RejoinMode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
