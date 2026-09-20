package org.lovetropics.games.common.core.game.util;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.IGameDefinition;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

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

	public static MutableComponent formatName(MutableComponent name) {
		return name.withStyle(ChatFormatting.ITALIC, ChatFormatting.GREEN);
	}

	public static MutableComponent formatStatus(MutableComponent message) {
		return message.withStyle(ChatFormatting.GOLD);
	}

	public static MutableComponent formatPositive(MutableComponent message) {
		return message.withStyle(ChatFormatting.AQUA);
	}

	public static MutableComponent formatNegative(MutableComponent message) {
		return message.withStyle(ChatFormatting.RED);
	}

	public static MutableComponent formatLink(Component link, String command) {
		Style style = Style.EMPTY
				.withUnderlined(true).withColor(ChatFormatting.BLUE)
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(command)));

		return link.copy().setStyle(style);
	}

	public static MutableComponent clickHere(String command) {
		return formatLink(CLICK_HERE, command);
	}

	public static MutableComponent gameName(IGameDefinition game) {
		return formatName(game.name().copy());
	}

	public static MutableComponent playerName(ServerPlayer player) {
		return player.getDisplayName().copy().withStyle(ChatFormatting.GREEN);
	}

	public static final class Commands {
		private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".command.");

		private static final TranslationCollector.Fun1 STOPPED_GAME = KEYS.add1("stopped_game", "You have stopped %s!");

		private static final TranslationCollector.Fun1 NO_TEAM = KEYS.add1("no_team", "No team with id: %s");

		public static final Component NOT_IN_GAME = KEYS.add("not_in_game", "You are not currently in any game!");

		public static final Component GLOBAL_CHAT_CHANNEL = KEYS.add("chat_channel.global", "Global Chat");
		public static final Component TEAM_CHAT_CHANNEL = KEYS.add("chat_channel.team", "Team Chat");
		public static final TranslationCollector.Fun1 SET_CHAT_CHANNEL = KEYS.add1("set_chat_channel", "You are now chatting in %s");
		public static final Component TEAM_CHAT_INTRO = KEYS.add("team_chat_intro", "You are using team chat. Use /shout or /chat global to chat with everyone.");

		private static final TranslationCollector.Fun3 STATISTIC_VALUE = KEYS.add3("statistic_value", "%s on %s has a value of: %s");
		public static final Component GAME_STATISTICS = KEYS.add("game_statistics", "Game Statistics");

		public static MutableComponent stoppedGame(IGameDefinition game) {
			return formatNegative(STOPPED_GAME.apply(gameName(game)));
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

		private static final Component INTEGRATIONS_NOT_CONNECTED = KEYS.add("integrations_not_connected", "Integrations socket is not connected!");

		public static MutableComponent integrationsNotConnected() {
			return formatNegative(INTEGRATIONS_NOT_CONNECTED.copy());
		}
	}

	public static final class Ui {
		private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".ui.");

		public static final Component FREE_CAMERA = KEYS.add("free_camera", "Free Camera");
		public static final TranslationCollector.Fun1 CLICK_TO_SELECT = KEYS.add1("click_to_select", "%s [Click to Select]");
	}
}
