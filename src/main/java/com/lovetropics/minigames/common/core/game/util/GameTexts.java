package com.lovetropics.minigames.common.core.game.util;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.BiConsumer;

public final class GameTexts {
	private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".");

	public static final Component CLICK_HERE = KEYS.add("click_here", "Click here");

	public static void collectTranslations(BiConsumer<String, String> consumer) {
		KEYS.forEach(consumer);
		Commands.KEYS.forEach(consumer);
		Ui.KEYS.forEach(consumer);
		Status.KEYS.forEach(consumer);
	}

	private static MutableComponent formatName(MutableComponent name) {
		return name.withStyle(ChatFormatting.ITALIC, ChatFormatting.GREEN);
	}

	private static MutableComponent formatStatus(MutableComponent message) {
		return message.withStyle(ChatFormatting.GOLD);
	}

	private static MutableComponent formatPositive(MutableComponent message) {
		return message.withStyle(ChatFormatting.AQUA);
	}

	private static MutableComponent formatNegative(MutableComponent message) {
		return message.withStyle(ChatFormatting.RED);
	}

	private static MutableComponent formatLink(Component link, String command) {
		Style style = Style.EMPTY
				.withUnderlined(true).withColor(ChatFormatting.BLUE)
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(command)));

		return link.copy().setStyle(style);
	}

	public static MutableComponent clickHere(String command) {
		return formatLink(CLICK_HERE, command);
	}

	public static MutableComponent lobbyName(GameLobby lobby) {
		return formatName(Component.literal(lobby.getMetadata().name()));
	}

	public static MutableComponent gameName(IGameDefinition game) {
		return formatName(game.name().copy());
	}

	public static MutableComponent playerName(ServerPlayer player) {
		return player.getDisplayName().copy().withStyle(ChatFormatting.GREEN);
	}

	public static final class Commands {
		private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".command.");

		private static final TranslationCollector.Fun1 JOINED_LOBBY = KEYS.add1("joined_lobby", "You have joined %s!");
		private static final TranslationCollector.Fun1 LEFT_LOBBY = KEYS.add1("left_lobby", "You have left %s!");

		private static final TranslationCollector.Fun1 STARTED_GAME = KEYS.add1("started_game", "You have started %s!");
		private static final TranslationCollector.Fun1 STOPPED_GAME = KEYS.add1("stopped_game", "You have stopped %s!");

		public static final Component ALREADY_IN_LOBBY = KEYS.add("already_in_lobby", "You have already joined a lobby!");
		public static final Component NO_JOINABLE_LOBBIES = KEYS.add("no_joinable_lobbies", "There are no public lobbies to join!");
		public static final Component CANNOT_START_LOBBY = KEYS.add("cannot_start", "There is no game to start in this lobby!");
		public static final Component NOT_IN_LOBBY = KEYS.add("not_in_lobby", "You are not currently in any lobby!");
		public static final Component NOT_IN_GAME = KEYS.add("not_in_game", "You are not currently in any game!");
		public static final Component GAME_ALREADY_STARTED = KEYS.add("game_already_started", "This game has already been started!");
		public static final Component GAME_ALREADY_STOPPED = KEYS.add("game_already_stopped", "This game has already been stopped!");
		public static final Component NO_MANAGE_PERMISSION = KEYS.add("no_manage_permission", "You do not have permission to manage this lobby!");
		private static final TranslationCollector.Fun1 NO_TEAM = KEYS.add1("no_team", "No team with id: %s");

		public static final Component GAMES_INTERSECT = KEYS.add("games_intersect", "The game cannot be started because it intersects with another active game!");

		private static final Component LOBBY_SELECTOR_HEADER = KEYS.add("lobby_selector_header", "There are multiple lobbies available to join! Select one from this list:");
		private static final TranslationCollector.Fun3 LOBBY_SELECTOR_ENTRY = KEYS.add3("lobby_selector_entry", "- %s (%s players): %s to join");

		private static final Component CANNOT_TELEPORT_INTO_GAME = KEYS.add("cannot_teleport_into_game", "You cannot teleport into a game without being a part of it!");

		public static final Component GLOBAL_CHAT_CHANNEL = KEYS.add("chat_channel.global", "Global Chat");
		public static final Component TEAM_CHAT_CHANNEL = KEYS.add("chat_channel.team", "Team Chat");
		public static final TranslationCollector.Fun1 SET_CHAT_CHANNEL = KEYS.add1("set_chat_channel", "You are now chatting in %s");
		public static final Component TEAM_CHAT_INTRO = KEYS.add("team_chat_intro", "You are using team chat. Use /shout or /chat global to chat with everyone.");

		private static final TranslationCollector.Fun3 STATISTIC_VALUE = KEYS.add3("statistic_value", "%s on %s has a value of: %s");
		public static final Component GAME_STATISTICS = KEYS.add("game_statistics", "Game Statistics");

		public static MutableComponent joinedLobby(GameLobby lobby) {
			return formatPositive(JOINED_LOBBY.apply(lobbyName(lobby)));
		}

		public static MutableComponent leftLobby(GameLobby lobby) {
			return formatNegative(LEFT_LOBBY.apply(lobbyName(lobby)));
		}

		public static MutableComponent startedGame(IGameDefinition game) {
			return formatPositive(STARTED_GAME.apply(gameName(game)));
		}

		public static MutableComponent stoppedGame(IGameDefinition game) {
			return formatNegative(STOPPED_GAME.apply(gameName(game)));
		}

		public static MutableComponent lobbySelector(Collection<? extends GameLobby> lobbies, @Nullable PlayerRole role) {
			MutableComponent selector = LOBBY_SELECTOR_HEADER.copy().append("\n")
					.withStyle(ChatFormatting.GOLD);

			for (GameLobby lobby : lobbies) {
				Component lobbyName = lobbyName(lobby);
				int players = lobby.getPlayers().size();
				Component link = clickHere(lobby.getMetadata().joinCommand(role));

				MutableComponent entry = LOBBY_SELECTOR_ENTRY.apply(lobbyName, players, link);
				selector = selector.append(entry.withStyle(ChatFormatting.GRAY)).append("\n");
			}

			return selector;
		}

		public static MutableComponent cannotTeleportIntoGame() {
			return formatNegative(CANNOT_TELEPORT_INTO_GAME.copy());
		}

		public static MutableComponent noTeam(Object id) {
			return NO_TEAM.apply(id);
		}

		public static <T> MutableComponent statisticValue(StatisticKey<T> statisticKey, Component targetName, @Nullable T value) {
			return STATISTIC_VALUE.apply(statisticKey.getKey(), targetName, value == null ? "null" : statisticKey.display(value));
		}
	}

	public static final class Status {
		private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".status.");

		private static final TranslationCollector.Fun2 LOBBY_OPENED = KEYS.add2("lobby_opened", "%s has opened for registration! %s to get a chance to play!");

		private static final TranslationCollector.Fun2 PLAYER_JOINED = KEYS.add2("player_joined", "%s has joined %s!");
		private static final TranslationCollector.Fun2 SPECTATOR_JOINED = KEYS.add2("spectator_joined", "%s has joined %s as a spectator!");

		private static final Component LEFT_GAME_DIMENSION = KEYS.add("left_game_dimension", "You left the game dimension and have been removed from the lobby!");

		private static final Component LOBBY_PAUSED = KEYS.add("lobby_paused", "Your current lobby has paused! You have been teleported to your last location.");
		private static final Component LOBBY_STOPPED = KEYS.add("lobby_stopped", "Your current lobby has stopped! You have been teleported to your last location.");

		private static final Component INTEGRATIONS_NOT_CONNECTED = KEYS.add("integrations_not_connected", "Integrations socket is not connected!");

		public static MutableComponent lobbyOpened(GameLobby lobby) {
			Component link = clickHere(lobby.getMetadata().joinCommand(null));
			return formatStatus(LOBBY_OPENED.apply(lobbyName(lobby), link));
		}

		public static MutableComponent playerJoined(GameLobby lobby, ServerPlayer player, @Nullable PlayerRole role) {
			TranslationCollector.Fun2 message = role != PlayerRole.SPECTATOR ? PLAYER_JOINED : SPECTATOR_JOINED;
			return formatPositive(message.apply(playerName(player), lobbyName(lobby)));
		}

		public static MutableComponent lobbyPaused() {
			return formatStatus(LOBBY_PAUSED.copy());
		}

		public static MutableComponent lobbyStopped() {
			return formatStatus(LOBBY_STOPPED.copy());
		}

		public static MutableComponent leftGameDimension() {
			return formatNegative(LEFT_GAME_DIMENSION.copy());
		}

		public static MutableComponent integrationsNotConnected() {
			return formatNegative(INTEGRATIONS_NOT_CONNECTED.copy());
		}
	}

	public static final class Ui {
		private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".ui.");

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
