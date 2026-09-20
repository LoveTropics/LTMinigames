package org.lovetropics.games.lobbies;

import org.lovetropics.games.common.core.game.IGameDefinition;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.util.GameTexts;
import org.lovetropics.games.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.BiConsumer;

public final class GameLobbyTexts {
	public static void collectTranslations(BiConsumer<String, String> consumer) {
		Commands.KEYS.forEach(consumer);
		Ui.KEYS.forEach(consumer);
		Status.KEYS.forEach(consumer);
	}

	public static MutableComponent lobbyName(GameLobby lobby) {
		return GameTexts.formatName(Component.literal(lobby.getMetadata().name()));
	}

	public static final class Commands {
		private static final TranslationCollector KEYS = new TranslationCollector(LobbiesMod.ID + ".command.");

		private static final TranslationCollector.Fun1 JOINED_LOBBY = KEYS.add1("joined_lobby", "You have joined %s!");
		private static final TranslationCollector.Fun1 LEFT_LOBBY = KEYS.add1("left_lobby", "You have left %s!");
		private static final TranslationCollector.Fun1 PLAYER_KICKED = KEYS.add1("player_kicked", "%s has been kicked from this lobby");

		private static final TranslationCollector.Fun1 STARTED_GAME = KEYS.add1("started_game", "You have started %s!");

		public static final Component ALREADY_IN_LOBBY = KEYS.add("already_in_lobby", "You have already joined a lobby!");
		public static final Component NO_JOINABLE_LOBBIES = KEYS.add("no_joinable_lobbies", "There are no public lobbies to join!");
		public static final Component CANNOT_START_LOBBY = KEYS.add("cannot_start", "There is no game to start in this lobby!");
		public static final Component NOT_IN_LOBBY = KEYS.add("not_in_lobby", "You are not currently in any lobby!");
		public static final Component GAME_ALREADY_STARTED = KEYS.add("game_already_started", "This game has already been started!");
		public static final Component GAME_ALREADY_STOPPED = KEYS.add("game_already_stopped", "This game has already been stopped!");
		public static final Component NO_MANAGE_PERMISSION = KEYS.add("no_manage_permission", "You do not have permission to manage this lobby!");

		private static final Component LOBBY_SELECTOR_HEADER = KEYS.add("lobby_selector_header", "There are multiple lobbies available to join! Select one from this list:");
		private static final TranslationCollector.Fun3 LOBBY_SELECTOR_ENTRY = KEYS.add3("lobby_selector_entry", "- %s (%s players): %s to join");

		private static final Component CANNOT_TELEPORT_INTO_GAME = KEYS.add("cannot_teleport_into_game", "You cannot teleport into a game without being a part of it!");

		public static final Component GAMES_INTERSECT = KEYS.add("games_intersect", "The game cannot be started because it intersects with another active game!");

		public static MutableComponent joinedLobby(GameLobby lobby) {
			return GameTexts.formatPositive(JOINED_LOBBY.apply(lobbyName(lobby)));
		}

		public static MutableComponent leftLobby(GameLobby lobby) {
			return GameTexts.formatNegative(LEFT_LOBBY.apply(lobbyName(lobby)));
		}

		public static MutableComponent playerKicked(ServerPlayer player) {
			return GameTexts.formatNegative(PLAYER_KICKED.apply(player.getDisplayName()));
		}

		public static MutableComponent startedGame(IGameDefinition game) {
			return GameTexts.formatPositive(STARTED_GAME.apply(GameTexts.gameName(game)));
		}

		public static MutableComponent lobbySelector(Collection<? extends GameLobby> lobbies, @Nullable PlayerRole role) {
			MutableComponent selector = LOBBY_SELECTOR_HEADER.copy().append("\n")
					.withStyle(ChatFormatting.GOLD);

			for (GameLobby lobby : lobbies) {
				Component lobbyName = lobbyName(lobby);
				int players = lobby.getPlayers().size();
				Component link = GameTexts.clickHere(lobby.getMetadata().joinCommand(role));

				MutableComponent entry = LOBBY_SELECTOR_ENTRY.apply(lobbyName, players, link);
				selector = selector.append(entry.withStyle(ChatFormatting.GRAY)).append("\n");
			}

			return selector;
		}

		public static MutableComponent cannotTeleportIntoGame() {
			return GameTexts.formatNegative(CANNOT_TELEPORT_INTO_GAME.copy());
		}
	}

	public static final class Status {
		private static final TranslationCollector KEYS = new TranslationCollector(LobbiesMod.ID + ".status.");

		private static final TranslationCollector.Fun2 LOBBY_OPENED = KEYS.add2("lobby_opened", "%s has opened for registration! %s to get a chance to play!");

		private static final TranslationCollector.Fun2 PLAYER_JOINED = KEYS.add2("player_joined", "%s has joined %s!");
		private static final TranslationCollector.Fun2 SPECTATOR_JOINED = KEYS.add2("spectator_joined", "%s has joined %s as a spectator!");

		private static final Component LEFT_GAME_DIMENSION = KEYS.add("left_game_dimension", "You left the game dimension and have been removed from the lobby!");

		private static final Component LOBBY_PAUSED = KEYS.add("lobby_paused", "Your current lobby has paused! You have been teleported to your last location.");
		private static final Component LOBBY_STOPPED = KEYS.add("lobby_stopped", "Your current lobby has stopped! You have been teleported to your last location.");

		public static MutableComponent lobbyOpened(GameLobby lobby) {
			Component link = GameTexts.clickHere(lobby.getMetadata().joinCommand(null));
			return GameTexts.formatStatus(LOBBY_OPENED.apply(lobbyName(lobby), link));
		}

		public static MutableComponent playerJoined(GameLobby lobby, ServerPlayer player, @Nullable PlayerRole role) {
			TranslationCollector.Fun2 message = role != PlayerRole.SPECTATOR ? PLAYER_JOINED : SPECTATOR_JOINED;
			return GameTexts.formatPositive(message.apply(GameTexts.playerName(player), lobbyName(lobby)));
		}

		public static MutableComponent lobbyPaused() {
			return GameTexts.formatStatus(LOBBY_PAUSED.copy());
		}

		public static MutableComponent lobbyStopped() {
			return GameTexts.formatStatus(LOBBY_STOPPED.copy());
		}

		public static MutableComponent leftGameDimension() {
			return GameTexts.formatNegative(LEFT_GAME_DIMENSION.copy());
		}
	}

	public static final class Ui {
		private static final TranslationCollector KEYS = new TranslationCollector(LobbiesMod.ID + ".ui.");

		public static final Component MANAGE_GAME_LOBBY = KEYS.add("manage_game_lobby", "Manage Game Lobby");
		public static final Component LOBBY_NAME = KEYS.add("lobby_name", "Lobby Name");
		public static final Component PUBLISH = KEYS.add("publish", "Publish");
		public static final Component FOCUS_LIVE = KEYS.add("focus_live", "Focus Live");
		public static final Component GAME_QUEUE = KEYS.add("game_queue", "Game Queue");
		public static final Component INSTALLED_GAMES = KEYS.add("installed_games", "Installed");
		public static final Component GAME_INACTIVE = KEYS.add("game_inactive", "Inactive");
		public static final Component CLOSE_LOBBY = KEYS.add("close_lobby", "Close");

		public static final Component SELECT_PLAYER_ROLE = KEYS.add("select_player_role", "Select Player Role");
		private static final TranslationCollector.Fun1 SELECT_ROLE_MESSAGE = KEYS.add1("select_role_message", "Welcome to the game lobby!\nBefore the game, %s.\n\nYou will be prompted before each game in this lobby.");
		private static final TranslationCollector.Fun2 SELECT_ROLE_MESSAGE_IMPORTANT = KEYS.add2("select_role_message_important", "Please select to %s or %s");
		public static final Component SELECT_PLAY = KEYS.add("select_play", "Play");
		public static final Component SELECT_SPECTATE = KEYS.add("select_spectate", "Spectate");

		private static final TranslationCollector.Fun1 GAME_PLAYER_COUNT = KEYS.add1("game_player_count", "%s players");

		public static final Component PARTICIPATING = KEYS.add("participating", "Participating");
		public static final Component SPECTATING = KEYS.add("spectating", "Spectating");

		public static final Component FREE_CAMERA = KEYS.add("free_camera", "Free Camera");
		public static final TranslationCollector.Fun1 CLICK_TO_SELECT = KEYS.add1("click_to_select", "%s [Click to Select]");

		public static final Component LOBBY_PUBLIC = KEYS.add("visibility.public", "Public");
		public static final Component LOBBY_PUBLIC_LIVE = KEYS.add("visibility.public_live", "Public (Live)");
		public static final Component LOBBY_PRIVATE = KEYS.add("visibility.private", "Private");

		public static MutableComponent playerCount(int count) {
			return GAME_PLAYER_COUNT.apply(count);
		}

		public static Component roleDescription(PlayerRole role) {
			if (role == PlayerRole.SPECTATOR) {
				return SPECTATING;
			} else {
				return PARTICIPATING;
			}
		}

		public static MutableComponent selectRoleMessage(Component play, Component spectate) {
			return SELECT_ROLE_MESSAGE.apply(
					SELECT_ROLE_MESSAGE_IMPORTANT.apply(play, spectate).withStyle(ChatFormatting.UNDERLINE)
			);
		}
	}
}
