package com.lovetropics.minigames.common.core.game.command;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.impl.GamePhase;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = LoveTropics.ID)
public class GameCommandManager {
	@SubscribeEvent
	public static void onCommandParse(CommandEvent event) {
		ParseResults<CommandSourceStack> parse = event.getParseResults();
		if (isParseSuccess(parse)) {
			// Already a successful parse, we're unlikely to be executing a command from a game
			return;
		}
		CommandSourceStack source = parse.getContext().getSource();
		GamePhase gamePhase = (GamePhase) GamePhaseManager.get().getGamePhaseFor(source);
		if (gamePhase == null) {
			return;
		}
		String commandString = parse.getReader().getString();
		ParseResults<CommandSourceStack> gameParse = gamePhase.getCommandSet().dispatcher().parse(commandString, source);
		// Heuristic based on vibes - if we parsed more than would be without the game commands, it's probably the error we want
		if (isParseSuccess(gameParse) || gameParse.getReader().getCursor() > parse.getReader().getCursor()) {
			event.setParseResults(gameParse);
		}
	}

	private static boolean isParseSuccess(ParseResults<CommandSourceStack> parse) {
		return !parse.getReader().canRead() && parse.getExceptions().isEmpty();
	}

	public static void addCommandsForClient(ServerPlayer player, RootCommandNode<CommandSourceStack> root) {
		GamePhase gamePhase = GamePhaseManager.get().getGamePhaseFor(player);
		if (gamePhase != null) {
			root.addChild(gamePhase.getCommandSet().baseCommand());
		}
	}

	@Nullable
	public static CompletableFuture<Suggestions> getCommandSuggestions(ParseResults<CommandSourceStack> parse) {
		if (isParseSuccess(parse)) {
			return null;
		}
		CommandSourceStack source = parse.getContext().getSource();
		GamePhase gamePhase = (GamePhase) GamePhaseManager.get().getGamePhaseFor(source);
		if (gamePhase == null) {
			return null;
		}
		StringReader gameReader = new StringReader(parse.getReader().getString());
		if (gameReader.canRead() && gameReader.peek() == '/') {
			gameReader.skip();
		}
		CommandDispatcher<CommandSourceStack> gameDispatcher = gamePhase.getCommandSet().dispatcher();
		return gameDispatcher.getCompletionSuggestions(gameDispatcher.parse(gameReader, source));
	}
}
