package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.google.common.primitives.Booleans;
import com.lovetropics.minigames.client.toast.NotificationIcon;
import com.lovetropics.minigames.client.toast.NotificationStyle;
import com.lovetropics.minigames.client.toast.ShowNotificationToastMessage;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.SpectatingClientState;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.role.StreamHosts;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.Team;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class SpectatorChaseBehavior implements IGameBehavior {
	public static final MapCodec<SpectatorChaseBehavior> CODEC = MapCodec.unit(SpectatorChaseBehavior::new);

	private static final NotificationStyle SPECTATING_NOTIFICATION_STYLE = new NotificationStyle(
			NotificationIcon.item(new ItemStack(Items.ENDER_EYE)),
			NotificationStyle.Sentiment.NEUTRAL,
			NotificationStyle.Color.LIGHT,
			10 * 1000
	);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (role == PlayerRole.SPECTATOR) {
				PacketDistributor.sendToPlayer(
						player,
						new ShowNotificationToastMessage(MinigameTexts.SPECTATING_NOTIFICATION, SPECTATING_NOTIFICATION_STYLE)
				);
			} else if (lastRole == PlayerRole.SPECTATOR) {
				removeSpectator(player);
			}
			sendSpectatingUpdate(game);
		});
		events.listen(GamePlayerEvents.REMOVE, player -> removePlayer(game, player));
		events.listen(GamePhaseEvents.DESTROY, () -> stop(game));
	}

	private void removePlayer(IGamePhase game, ServerPlayer player) {
		removeSpectator(player);

		sendSpectatingUpdate(game);
	}

	private void sendSpectatingUpdate(IGamePhase game) {
		SpectatingClientState spectating = buildSpectatingState(game);
		GameClientState.sendToPlayers(spectating, game.spectators());
	}

	private void removeSpectator(ServerPlayer player) {
		GameClientState.removeFromPlayer(GameClientStateTypes.SPECTATING.get(), player);
	}

	private void stop(IGamePhase game) {
		GameClientState.removeFromPlayers(GameClientStateTypes.SPECTATING.get(), game.spectators());
	}

	private SpectatingClientState buildSpectatingState(IGamePhase game) {
		PlayerSet participants = game.participants();

		Comparator<ServerPlayer> comparator = Comparator
				.<ServerPlayer, Boolean>comparing(StreamHosts::isHost, Booleans.trueFirst())
				.thenComparing((ServerPlayer player) -> {
					Team team = player.getTeam();
					return team != null ? team.getName() : "";
				})
				.thenComparing(ServerPlayer::getScoreboardName, String::compareToIgnoreCase);

		List<UUID> ids = participants.stream()
				.sorted(comparator)
				.map(ServerPlayer::getUUID)
				.toList();
		return new SpectatingClientState(ids);
	}
}
