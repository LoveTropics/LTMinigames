package org.lovetropics.games.voice.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameTeamEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.voice.GamesVoicePlugin;

import java.util.HashMap;
import java.util.Map;

public record VoiceChatBehavior(
		boolean openToEveryone,
		boolean groupByTeam,
		GroupProximityMode groupProximityMode,
		boolean includeSpectatorsInGroup
) implements IGameBehavior {
	private static final String NODE_LISTEN = "voicechat.listen";
	private static final String NODE_SPEAK = "voicechat.speak";

	public static final MapCodec<VoiceChatBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.fieldOf("open_to_everyone").forGetter(VoiceChatBehavior::openToEveryone),
			Codec.BOOL.optionalFieldOf("group_by_team", false).forGetter(VoiceChatBehavior::groupByTeam),
			GroupProximityMode.CODEC.optionalFieldOf("group_proximity_mode", GroupProximityMode.NORMAL).forGetter(VoiceChatBehavior::groupProximityMode),
			Codec.BOOL.optionalFieldOf("include_spectators_in_group", false).forGetter(VoiceChatBehavior::includeSpectatorsInGroup)
	).apply(i, VoiceChatBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		// Note: checkers may be called from off-thread, this is important to keep low latency in audio processing
		if (openToEveryone) {
			game.permissionNodes().registerAnyThread(NODE_LISTEN, PermissionTypes.BOOLEAN,
					(_, _, _) -> true
			);
			game.permissionNodes().registerAnyThread(NODE_SPEAK, PermissionTypes.BOOLEAN,
					(_, _, _) -> true
			);
		}

		if (groupByTeam) {
			TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
			Map<GameTeamKey, Group> groupsByTeam = new HashMap<>();

			VoicechatServerApi voiceApi = GamesVoicePlugin.voiceApi();

			events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (_, _) -> {
				for (GameTeamKey team : teams.getTeamKeys()) {
					// Intentionally not hiding the group/setting a password for now
					// Joining a group is already a privileged action, and it's nice to allow organisers to jump between chats if we need to
					groupsByTeam.put(team, voiceApi.groupBuilder()
							.setName(team.id())
							.setType(groupProximityMode.asType())
							.build());
				}
			});

			events.listen(GameTeamEvents.SET_GAME_TEAM, (player, _, team) ->
					addToGroup(player, voiceApi, groupsByTeam.get(team))
			);

			events.listen(GamePlayerEvents.SET_ROLE, (player, role, _) -> {
				Group group = getGroup(player, teams, groupsByTeam);
				if (group != null) {
					if (includeSpectatorsInGroup || role == PlayerRole.PARTICIPANT) {
						addToGroup(player, voiceApi, group);
					} else {
						removeFromGroup(player, voiceApi, group);
					}
				}
			});

			events.listen(GamePlayerEvents.REMOVE, player -> {
				Group group = getGroup(player, teams, groupsByTeam);
				if (group != null) {
					removeFromGroup(player, voiceApi, group);
				}
			});

			events.listen(GamePhaseEvents.DESTROY, () -> {
				for (Group group : groupsByTeam.values()) {
					voiceApi.removeGroup(group.getId());
				}
			});
		}
	}

	private @Nullable Group getGroup(ServerPlayer player, TeamState teams, Map<GameTeamKey, Group> groupsByTeam) {
		GameTeamKey team = teams.getTeamForPlayer(player);
		return team != null ? groupsByTeam.get(team) : null;
	}

	private void addToGroup(ServerPlayer player, VoicechatServerApi voiceApi, Group group) {
		VoicechatConnection connection = voiceApi.getConnectionOf(player.getUUID());
		if (connection != null) {
			connection.setGroup(group);
		}
	}

	private void removeFromGroup(ServerPlayer player, VoicechatServerApi voiceApi, Group group) {
		VoicechatConnection connection = voiceApi.getConnectionOf(player.getUUID());
		if (connection != null && connection.getGroup() == group) {
			connection.setGroup(null);
		}
	}

	public enum GroupProximityMode implements StringRepresentable {
		NORMAL("normal"),
		OPEN("open"),
		ISOLATED("isolated"),
		;

		public static final Codec<GroupProximityMode> CODEC = StringRepresentable.fromEnum(GroupProximityMode::values);

		private final String name;

		GroupProximityMode(String name) {
			this.name = name;
		}

		public Group.Type asType() {
			return switch (this) {
				case NORMAL -> Group.Type.NORMAL;
				case OPEN -> Group.Type.OPEN;
				case ISOLATED -> Group.Type.ISOLATED;
			};
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
