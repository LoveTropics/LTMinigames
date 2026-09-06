package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.PlayerIsolation;
import com.lovetropics.minigames.common.core.game.lobby.GameLobbyId;
import com.lovetropics.minigames.common.core.game.lobby.GameLobbyMetadata;
import com.lovetropics.minigames.common.core.game.lobby.LobbyVisibility;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EventBusSubscriber(modid = LoveTropics.ID)
public class GameLobbyManager {
	private static final GameLobbyManager INSTANCE = new GameLobbyManager();

	private final List<GameLobby> lobbies = new ArrayList<>();

	private final Map<UUID, GameLobby> lobbiesByPlayer = new Object2ObjectOpenHashMap<>();

	@Nullable
	private GameLobby focusedLiveLobby;

	public static GameLobbyManager get() {
		return INSTANCE;
	}

	public GameResult<GameLobby> createGameLobby(String name, ServerPlayer initiator) {
		GameLobby currentLobby = lobbiesByPlayer.get(initiator.getUUID());
		if (currentLobby != null) {
			return GameResult.error(GameTexts.Commands.ALREADY_IN_LOBBY);
		}

		GameLobbyId id = GameLobbyId.next();
		GameLobbyMetadata metadata = new GameLobbyMetadata(id, PlayerKey.from(initiator), name);

		GameLobby lobby = new GameLobby(this, initiator.level().getServer(), metadata);
		lobbies.add(lobby);

		return GameResult.ok(lobby);
	}

	@Nullable
	public GameLobby getLobbyFor(Player player) {
		if (player.level().isClientSide()) {
			return null;
		}
		return lobbiesByPlayer.get(player.getUUID());
	}

	@Nullable
	public GameLobby getLobbyFor(CommandSourceStack source) {
		if (source.getEntity() instanceof Player player) {
			return getLobbyFor(player);
		}
		return null;
	}

	@Nullable
	public GameLobby getLobby(Predicate<GameLobby> pred) {
		return lobbies.stream().filter(pred).findFirst().orElse(null);
	}

	public Collection<? extends GameLobby> getAllLobbies() {
		return lobbies;
	}

	@Nullable
	public GameLobby getLobbyByNetworkId(int id) {
		for (GameLobby lobby : lobbies) {
			if (lobby.getMetadata().id().networkId() == id) {
				return lobby;
			}
		}
		return null;
	}

	@Nullable
	public GameLobby getLobbyById(UUID id) {
		for (GameLobby lobby : lobbies) {
			if (lobby.getMetadata().id().uuid().equals(id)) {
				return lobby;
			}
		}
		return null;
	}

	void addPlayerToLobby(ServerPlayer player, GameLobby lobby) {
		lobbiesByPlayer.put(player.getUUID(), lobby);
	}

	void removePlayerFromLobby(ServerPlayer player, GameLobby lobby) {
		lobbiesByPlayer.remove(player.getUUID(), lobby);
	}

	void removeLobby(GameLobby lobby) {
		lobbies.remove(lobby);

		if (focusedLiveLobby == lobby) {
			setFocusedLiveLobby(null);
		}
	}

	GameLobbyMetadata setVisibility(GameLobby lobby, LobbyVisibility visibility) {
		if (visibility.isFocusedLive()) {
			if (!setFocusedLive(lobby)) {
				return lobby.metadata;
			}
		}

		return lobby.metadata.withVisibility(visibility);
	}

	private boolean setFocusedLive(GameLobby lobby) {
		if (focusedLiveLobby == null) {
			setFocusedLiveLobby(lobby);
			return true;
		} else {
			return false;
		}
	}

	private void setFocusedLiveLobby(@Nullable GameLobby lobby) {
		focusedLiveLobby = lobby;

		for (GameLobby otherLobby : lobbies) {
			otherLobby.management.onFocusedLiveLobbyChanged();
		}
	}

	boolean hasFocusedLiveLobby() {
		return focusedLiveLobby != null;
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		List<GameLobby> lobbies = new ArrayList<>(INSTANCE.lobbies);
		for (GameLobby lobby : lobbies) {
			lobby.close(true);
		}
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		for (GameLobby lobby : INSTANCE.lobbies) {
			lobby.tick();
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		ServerPlayer player = (ServerPlayer) event.getEntity();
		if (!PlayerIsolation.INSTANCE.isReloading(player)) {
			for (GameLobby lobby : INSTANCE.lobbies) {
				lobby.onPlayerLoggedIn(player);
			}
		}
	}

	/// When a player logs out, remove them from the currently running game instance
	/// if they are inside, and teleport back them to their original state.
	///
	/// Also if they have registered for a game poll, they will be removed from the
	/// list of registered players.
	public static ServerPlayer onPlayerLoggedOut(ServerPlayer player) {
		if (!PlayerIsolation.INSTANCE.isReloading(player)) {
			for (GameLobby lobby : INSTANCE.lobbies) {
				player = lobby.onPlayerLoggedOut(player);
			}
		}
		return player;
	}

	public Stream<? extends GameLobby> getVisibleLobbies(CommandSourceStack source) {
		return getAllLobbies().stream()
				.filter(lobby -> lobby.isVisibleTo(source));
	}

	@SubscribeEvent
	public static void onPlayerTryChangeDimension(EntityTravelToDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		ServerLevel targetLevel = player.level().getServer().getLevel(event.getDimension());
		if (targetLevel == null) {
			return;
		}

		GameLobby lobby = INSTANCE.getLobbyFor(player);
		GamePhase targetPhase = GamePhaseManager.get().getGamePhaseInDimension(targetLevel);
		if (targetPhase != null && targetPhase.game.lobby() != lobby) {
			player.sendSystemMessage(GameTexts.Commands.cannotTeleportIntoGame(), true);
			event.setCanceled(true);
		}
	}

	@Nullable
	public static ServerPlayer onPlayerTeleport(ServerPlayer player, TeleportTransition transition) {
		GameLobby lobby = INSTANCE.getLobbyFor(player);
		GamePhase targetPhase = GamePhaseManager.get().getGamePhaseAt(transition.newLevel(), transition.position());
		if (targetPhase != null && targetPhase.game.lobby() != lobby) {
			player.sendSystemMessage(GameTexts.Commands.cannotTeleportIntoGame(), true);
			return player;
		}
		GamePhase playerPhase = GamePhaseManager.get().getGamePhaseFor(player);
		if (targetPhase == null || playerPhase == targetPhase) {
			return null;
		}
		return targetPhase.teleportFrom(player, playerPhase);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		GameLobby lobby = GameLobbyManager.get().getLobbyFor(player);
		if (lobby == null) {
			return;
		}

		Set<ResourceKey<Level>> validDimensions = lobby.allSubPhases()
				.map(game -> game.level().dimension())
				.collect(Collectors.toSet());

		if (validDimensions.contains(event.getFrom()) && !validDimensions.contains(event.getTo())) {
			if (lobby.getPlayers().remove(player, false)) {
				player.sendSystemMessage(GameTexts.Status.leftGameDimension(), false);
			}
		}
	}
}
