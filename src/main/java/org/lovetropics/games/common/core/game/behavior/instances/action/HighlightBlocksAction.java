package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.HighlightBlocksState;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record HighlightBlocksAction(
		RegionQuery region,
		boolean highlight
) implements IGameBehavior {
	public static final MapCodec<HighlightBlocksAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("regions").forGetter(HighlightBlocksAction::region),
			Codec.BOOL.fieldOf("highlight").forGetter(HighlightBlocksAction::highlight)
	).apply(i, HighlightBlocksAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		SharedState sharedState = SharedState.getOrAdd(game, events);
		events.listen(GameActionEvents.APPLY, (_, _) -> {
			if (highlight) {
				region.forEachBlock(game, (level, pos) -> sharedState.add(level.dimension(), pos));
			} else {
				region.forEachBlock(game, (level, pos) -> sharedState.remove(level.dimension(), pos));
			}
			sharedState.sendTo(game.allPlayers());
			return true;
		});
	}

	private static class SharedState implements IGameState {
		public static final GameStateKey<SharedState> KEY = GameStateKey.create("Highlight blocks");

		private final Map<ResourceKey<Level>, Set<BlockPos>> positionsByDimension = new Reference2ObjectOpenHashMap<>();

		public static SharedState getOrAdd(IGamePhase game, EventRegistrar events) {
			SharedState state = game.state().getOrNull(KEY);
			if (state != null) {
				return state;
			}
			SharedState newState = new SharedState();
			events.listen(GamePlayerEvents.ADD_TO_LEVEL, (player, level) ->
					GameClientState.sendToPlayer(newState.createClientState(level.dimension()), player)
			);
			events.listen(GamePlayerEvents.REMOVE, player ->
					GameClientState.removeFromPlayer(GameClientStateTypes.HIGHLIGHT_BLOCKS.get(), player)
			);
			game.state().register(KEY, newState);
			return newState;
		}

		public void add(ResourceKey<Level> dimension, BlockPos pos) {
			positionsByDimension.computeIfAbsent(dimension, _ -> new HashSet<>())
					.add(pos);
		}

		public void remove(ResourceKey<Level> dimension, BlockPos pos) {
			Set<BlockPos> positions = positionsByDimension.get(dimension);
			if (positions != null) {
				positions.remove(pos);
			}
		}

		public void sendTo(PlayerSet players) {
			for (ServerPlayer player : players) {
				HighlightBlocksState state = createClientState(player.level().dimension());
				GameClientState.sendToPlayer(state, player);
			}
		}

		private HighlightBlocksState createClientState(ResourceKey<Level> dimension) {
			Set<BlockPos> positions = positionsByDimension.get(dimension);
			return new HighlightBlocksState(positions != null ? List.copyOf(positions) : List.of());
		}
	}
}
