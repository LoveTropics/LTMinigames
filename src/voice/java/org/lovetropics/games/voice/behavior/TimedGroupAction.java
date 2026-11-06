package org.lovetropics.games.voice.behavior;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.voice.GamesVoicePlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TimedGroupAction(
		int seconds
) implements IGameBehavior {
	public static final MapCodec<TimedGroupAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.POSITIVE_INT.fieldOf("seconds").forGetter(TimedGroupAction::seconds)
	).apply(i, TimedGroupAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		VoicechatServerApi voiceApi = GamesVoicePlugin.voiceApi();

		Map<UUID, State> playerStates = new HashMap<>();
		// Leaked until the end of the game for now. Oh well.
		List<Group> groups = new ArrayList<>();

		events.listen(GameActionEvents.APPLY, (_, targets) -> {
			// Only succeed if all players can be added
			List<VoicechatConnection> connections = new ArrayList<>();
			for (ServerPlayer player : targets.asPlayers(game)) {
				VoicechatConnection connection = voiceApi.getConnectionOf(player.getUUID());
				if (connection == null) {
					continue;
				}
				if (playerStates.containsKey(player.getUUID())) {
					return false;
				}
				connections.add(connection);
			}
			if (connections.isEmpty()) {
				return false;
			}
			Group group = voiceApi.groupBuilder()
					.setName("timed_group_" + groups.size())
					.setType(Group.Type.OPEN)
					.build();
			groups.add(group);
			long endTime = game.ticks() + (long) seconds * SharedConstants.TICKS_PER_SECOND;
			for (VoicechatConnection connection : connections) {
				playerStates.put(connection.getPlayer().getUuid(), new State(endTime, group, connection.getGroup()));
				connection.setGroup(group);
			}
			return true;
		});

		events.listen(GamePhaseEvents.TICK, () -> playerStates.entrySet().removeIf(entry -> {
			State state = entry.getValue();
			if (game.ticks() <= state.endTime) {
				return false;
			}
			ServerPlayer player = game.allPlayers().getPlayerBy(entry.getKey());
			if (player != null) {
				restoreToGroup(player, voiceApi, state.group, state.lastGroup);
			}
			return true;
		}));

		events.listen(GamePlayerEvents.REMOVE, player -> {
			State state = playerStates.remove(player.getUUID());
			if (state != null) {
				restoreToGroup(player, voiceApi, state.group, null);
			}
		});

		events.listen(GamePhaseEvents.DESTROY, () -> {
			for (Group group : groups) {
				voiceApi.removeGroup(group.getId());
			}
		});
	}

	private void restoreToGroup(ServerPlayer player, VoicechatServerApi voiceApi, Group group, @Nullable Group newGroup) {
		VoicechatConnection connection = voiceApi.getConnectionOf(player.getUUID());
		if (connection != null && group.equals(connection.getGroup())) {
			connection.setGroup(newGroup);
		}
	}

	private record State(long endTime, Group group, @Nullable Group lastGroup) {
	}
}
