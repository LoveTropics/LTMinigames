package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.HighlightBlocksState;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record HighlightBlocksAction(
		List<String> regionKeys,
		boolean highlight
) implements IGameBehavior {
	public static final MapCodec<HighlightBlocksAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions").forGetter(HighlightBlocksAction::regionKeys),
			Codec.BOOL.fieldOf("highlight").forGetter(HighlightBlocksAction::highlight)
	).apply(i, HighlightBlocksAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<BlockBox> boxes = game.mapRegions().getAll(regionKeys);
		SharedState sharedState = SharedState.get(game);

		events.listen(GamePlayerEvents.ADD, player ->
				// Multiple actions will do this - but we deduplicate them. That sucks, but ok
				sharedState.sendTo(PlayerSet.of(player))
		);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			if (highlight) {
				for (BlockBox box : boxes) {
					for (BlockPos pos : box) {
						sharedState.add(pos);
					}
				}
			} else {
				for (BlockBox box : boxes) {
					for (BlockPos pos : box) {
						sharedState.remove(pos);
					}
				}
			}
			sharedState.sendTo(game.allPlayers());
			return true;
		});
	}

	private static class SharedState implements IGameState {
		public static final GameStateKey.Defaulted<SharedState> KEY = GameStateKey.create("Highlight blocks", SharedState::new);

		private final List<BlockPos> positions = new ArrayList<>();

		public static SharedState get(IGamePhase game) {
			return game.state().get(KEY);
		}

		public void add(BlockPos pos) {
			if (!positions.contains(pos)) {
				positions.add(pos);
			}
		}

		public void remove(BlockPos pos) {
			positions.remove(pos);
		}

		public void sendTo(PlayerSet players) {
			GameClientState.sendToPlayers(new HighlightBlocksState(List.copyOf(positions)), players);
		}
	}
}
