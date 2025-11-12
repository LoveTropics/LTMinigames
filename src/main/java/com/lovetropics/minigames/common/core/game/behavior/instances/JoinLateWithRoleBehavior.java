package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerRoleSelections;
import com.lovetropics.minigames.common.core.game.player.PlayerStorage;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;

public record JoinLateWithRoleBehavior(PlayerRole role, boolean allowRejoin) implements IGameBehavior {
	public static final MapCodec<JoinLateWithRoleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlayerRole.CODEC.fieldOf("role").forGetter(c -> c.role),
			Codec.BOOL.optionalFieldOf("allow_rejoin", false).forGetter(c -> c.allowRejoin)
	).apply(i, JoinLateWithRoleBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		final TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		final Map<PlayerKey, OldParticipant> oldParticipants = new Object2ObjectOpenHashMap<>();

		events.listen(GamePlayerEvents.SELECT_ROLE_ON_JOIN, (player, selectedRole) -> {
			// Let the player be a spectator if they really want to
			if (selectedRole == PlayerRole.SPECTATOR) {
				return PlayerRole.SPECTATOR;
			}

			OldParticipant oldParticipant = oldParticipants.remove(player);
			if (allowRejoin && oldParticipant != null) {
				// Yes, ok - we're not supposed to fetch the player before they join.
				// But this whole player setup process is jank, and it's 5 days to the event. I made this mess, I can make it worse!
				final ServerPlayer playerEntity = game.allPlayers().getPlayerBy(player.id());
				if (playerEntity != null && teams != null && oldParticipant.team != null) {
					teams.addPlayerTo(playerEntity, oldParticipant.team);
				}
				return PlayerRole.PARTICIPANT;
			}

			return role;
		});

		if (allowRejoin) {
			events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
				final PlayerKey playerKey = PlayerKey.from(player);
				if (role == PlayerRole.PARTICIPANT) {
					final GameTeamKey team = teams != null ? teams.getTeamForPlayer(player) : null;
					oldParticipants.putIfAbsent(playerKey, new OldParticipant(team));
				} else if (lastRole == PlayerRole.PARTICIPANT) {
					oldParticipants.remove(playerKey);
				}
			});

			// TODO: We would ideally have much more clearly defined flow for a player that rejoins - e.g. a game with death should have the player effectively die
			final PlayerStorage playerStorage = new PlayerStorage();
			events.listen(GamePlayerEvents.LEAVE, player -> {
				if (game.participants().contains(player)) {
					try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
						TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
						player.saveWithoutId(output);
						playerStorage.setPlayerData(player, output.buildResult());
					}
				}
			});

			events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
				if (role == PlayerRole.PARTICIPANT) {
					playerStorage.fetchAndRemovePlayerData(playerId).ifPresent(spawn::loadFromTag);
				}
			});
		}
	}

	private record OldParticipant(@Nullable GameTeamKey team) {
	}
}
