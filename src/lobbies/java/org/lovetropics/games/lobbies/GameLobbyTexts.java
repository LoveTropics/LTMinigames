package org.lovetropics.games.lobbies;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGameDefinition;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.util.GameTexts;
import org.lovetropics.games.common.core.game.util.TranslationCollector;

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
		private static final TranslationCollector.Fun1 PLAYERS = KEYS.add1("players", "Players (%s)");
		public static final Component LOBBY_LOCKED = KEYS.add("lobby_locked", "Private");
		public static final Component LOBBY_UNLOCKED = KEYS.add("lobby_unlocked", "Public");
		public static final Component ADD_PLAYER = KEYS.add("add_player", "Add Player");
		public static final Component ADD_AS_PARTICIPANT = KEYS.add("add_as_participant", "Add as Participant");
		public static final Component ADD_AS_SPECTATOR = KEYS.add("add_as_spectator", "Add as Spectator");
		public static final Component KICK_PLAYER = KEYS.add("kick_player", "Kick player");
		public static final Component SEARCH_PLAYERS = KEYS.add("search_players", "Search players...");
		public static final Component LOADING_PLAYERS = KEYS.add("loading_players", "Loading players...");
		public static final Component NO_PLAYERS_TO_ADD = KEYS.add("no_players_to_add", "No players to add");
		private static final TranslationCollector.Fun1 IN_LOBBY = KEYS.add1("in_lobby", "In %s");
		private static final TranslationCollector.Fun1 CONFIRM_MOVE_PLAYER_TITLE = KEYS.add1("confirm_move_player.title", "Move %s to this lobby?");
		private static final TranslationCollector.Fun2 CONFIRM_MOVE_PLAYER_MESSAGE = KEYS.add2("confirm_move_player.message", "%s is currently in %s and will be removed from it.");
		public static final Component FOCUS_LIVE = KEYS.add("focus_live", "Focus Live");
		public static final Component GAME_QUEUE = KEYS.add("game_queue", "Game Queue");
		public static final Component INSTALLED_GAMES = KEYS.add("installed_games", "Installed");
		public static final Component SEARCH_GAMES = KEYS.add("search_games", "Search...");
		public static final Component ADD_TO_QUEUE = KEYS.add("add_to_queue", "Add to Queue");
		public static final Component BACK_TO_QUEUE = KEYS.add("back_to_queue", "Back to Queue");
		public static final Component ADD_GAME = KEYS.add("add_game", "Add a minigame...");
		public static final Component REMOVE_FROM_QUEUE = KEYS.add("remove_from_queue", "Remove from Queue");
		public static final Component PLAY_NOW = KEYS.add("play_now", "Play Now");
		public static final Component START_GAME = KEYS.add("start_game", "Start Game");
		public static final Component RESTART_GAME = KEYS.add("restart_game", "Restart Game");
		public static final Component STOP_GAME = KEYS.add("stop_game", "Stop Game");
		public static final Component CONFIRM_RESTART_GAME_TITLE = KEYS.add("confirm_restart_game.title", "Restart the current game?");
		public static final Component CONFIRM_RESTART_GAME_MESSAGE = KEYS.add("confirm_restart_game.message", "The current game will be restarted from the beginning.");
		public static final Component CONFIRM_PLAY_NOW_TITLE = KEYS.add("confirm_play_now.title", "Play this game now?");
		private static final Component CONFIRM_PLAY_NOW_STOPS_GAME = KEYS.add("confirm_play_now.stops_game", "The current game will be stopped and skipped.");
		private static final Component CONFIRM_PLAY_NOW_SKIPS_GAMES = KEYS.add("confirm_play_now.skips_games", "The game(s) queued inbetween will be skipped.");
		private static final Component CONFIRM_PLAY_NOW_STOPS_AND_SKIPS = KEYS.add("confirm_play_now.stops_and_skips", "The current game will be stopped and skipped, and the game(s) queued inbetween will be skipped.");
		public static final Component CONFIRM_STOP_GAME_TITLE = KEYS.add("confirm_stop_game.title", "Stop the current game?");
		public static final Component CONFIRM_STOP_GAME_MESSAGE = KEYS.add("confirm_stop_game.message", "Players will leave the game and the queue will pause.");
		public static final Component GAME_INACTIVE = KEYS.add("game_inactive", "Inactive");
		public static final Component CLOSE_LOBBY = KEYS.add("close_lobby", "Close");
		public static final Component CONFIRM_FOCUS_LIVE_TITLE = KEYS.add("confirm_focus_live.title", "Are you sure to focus this lobby live?");
		public static final Component CONFIRM_FOCUS_LIVE_MESSAGE = KEYS.add("confirm_focus_live.message", "Only one lobby can be live at a time.");
		public static final Component CONFIRM_CLOSE_LOBBY_TITLE = KEYS.add("confirm_close_lobby.title", "Close this lobby?");
		public static final Component CONFIRM_CLOSE_LOBBY_MESSAGE = KEYS.add("confirm_close_lobby.message", "The lobby will be removed.");

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

		public static Component confirmPlayNowMessage(boolean stopsGame, boolean skipsGames) {
			if (stopsGame) {
				return skipsGames ? CONFIRM_PLAY_NOW_STOPS_AND_SKIPS : CONFIRM_PLAY_NOW_STOPS_GAME;
			}
			return CONFIRM_PLAY_NOW_SKIPS_GAMES;
		}

		public static MutableComponent inLobby(String lobbyName) {
			return IN_LOBBY.apply(lobbyName);
		}

		public static MutableComponent confirmMovePlayerTitle(String playerName) {
			return CONFIRM_MOVE_PLAYER_TITLE.apply(playerName);
		}

		public static MutableComponent confirmMovePlayerMessage(String playerName, String lobbyName) {
			return CONFIRM_MOVE_PLAYER_MESSAGE.apply(playerName, lobbyName);
		}

		public static MutableComponent players(int count) {
			return PLAYERS.apply(count);
		}

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
