package com.lovetropics.minigames.common.core.game.behavior.instances.team;

import com.lovetropics.minigames.common.core.chat.ChatChannel;
import com.lovetropics.minigames.common.core.chat.ChatChannelStore;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.mutable.MutableBoolean;

public record TeamChatBehavior(ResourceKey<ChatType> chatType, boolean includeSpectators) implements IGameBehavior {
	public static final MapCodec<TeamChatBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceKey.codec(Registries.CHAT_TYPE).fieldOf("chat_type").forGetter(TeamChatBehavior::chatType),
			Codec.BOOL.optionalFieldOf("include_spectators", true).forGetter(TeamChatBehavior::includeSpectators)
	).apply(i, TeamChatBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		MutableBoolean gameOver = new MutableBoolean();
		events.listen(GamePlayerEvents.ADD, player -> {
			ChatChannelStore.set(player, ChatChannel.TEAM);
			player.sendSystemMessage(GameTexts.Commands.TEAM_CHAT_INTRO);
		});
		events.listen(GamePlayerEvents.CHAT, (player, signedMessage) -> {
			ChatChannel channel = ChatChannelStore.get(player);
			if (channel != ChatChannel.TEAM || gameOver.getValue()) {
				return false;
			}
			GameTeamKey teamKey = teams.getTeamForPlayer(player);
			if (teamKey != null) {
				GameTeam team = teams.getTeamByKey(teamKey);
				if (team != null) {
					broadcastToTeam(player, signedMessage, game, team);
					return true;
				}
			}
			return false;
		});
		events.listen(GameLogicEvents.GAME_OVER, winner -> gameOver.setTrue());
	}

	private void broadcastToTeam(ServerPlayer player, PlayerChatMessage signedMessage, IGamePhase game, GameTeam team) {
		ChatType.Bound chatType = ChatType.bind(this.chatType, player)
				.withTargetName(team.config().name().copy().withStyle(team.config().formatting()));

		player.level().getServer().logChatMessage(signedMessage.decoratedContent(), chatType, null);
		OutgoingChatMessage message = OutgoingChatMessage.create(signedMessage);

		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		for (ServerPlayer otherPlayer : teams.getParticipantsForTeam(game, team.key())) {
			otherPlayer.sendChatMessage(message, false, chatType);
		}

		if (includeSpectators) {
			for (ServerPlayer spectator : game.spectators()) {
				spectator.sendChatMessage(message, false, chatType);
			}
		}
	}
}
