package org.lovetropics.games.common.core.game.util;

import eu.pb4.placeholders.api.Placeholder;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;

import java.util.HashMap;
import java.util.Map;

public class GamePlaceholders implements IGameState {
	private static final GameStateKey.Defaulted<GamePlaceholders> KEY = GameStateKey.create("Placeholders", GamePlaceholders::new);

	private static final Map<Identifier, GamePlaceholder> REGISTERED_PLACEHOLDERS = new HashMap<>();

	private final Map<Identifier, Placeholder.Handler<ServerPlaceholderContext, String>> handlers = new HashMap<>();

	public static GamePlaceholders get(IGamePhase game) {
		return game.state().get(KEY);
	}

	private GamePlaceholder registerPlaceholder(Identifier id) {
		GamePlaceholder placeholder = new GamePlaceholder(id);
		Placeholders.registerServer(id, placeholder);
		return placeholder;
	}

	public void register(Identifier id, Placeholder.Handler<ServerPlaceholderContext, String> handler) {
		REGISTERED_PLACEHOLDERS.computeIfAbsent(id, this::registerPlaceholder);
		handlers.put(id, handler);
	}

	private record GamePlaceholder(Identifier id) implements Placeholder.Handler<ServerPlaceholderContext, String> {
		@Override
		public PlaceholderResult onPlaceholderRequest(ServerPlaceholderContext context, String argument) {
			IGamePhase game = getGameFor(context);
			if (game != null) {
				Placeholder.Handler<ServerPlaceholderContext, String> handler = get(game).handlers.get(id);
				return handler != null ? handler.onPlaceholderRequest(context, argument) : PlaceholderResult.invalid();
			}
			return PlaceholderResult.invalid();
		}

		private @Nullable IGamePhase getGameFor(ServerPlaceholderContext context) {
			IGameLookup gameLookup = IGameLookup.get();
			ServerPlayer player = context.serverPlayer();
			ServerLevel level = context.serverLevel();
			Vec3 position = context.position();
			if (player != null) {
				return gameLookup.getGamePhaseFor(player);
			} else if (level != null && position != null) {
				return gameLookup.getGamePhaseAt(level, position);
			} else if (level != null) {
				return gameLookup.getGamePhaseInDimension(level);
			}
			return null;
		}
	}
}
