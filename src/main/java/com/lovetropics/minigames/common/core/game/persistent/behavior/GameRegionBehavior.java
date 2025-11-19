package com.lovetropics.minigames.common.core.game.persistent.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.lovetropics.minigames.common.core.map.SavedRegions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GameRegionBehavior implements PersistentGameBehavior {
	public static final MapCodec<GameRegionBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
	        Codec.STRING.fieldOf("region").forGetter(b -> b.region)
	).apply(instance, GameRegionBehavior::new));

	private final String region;

	private final List<BlockBox> regions = new ArrayList<>();

	public GameRegionBehavior(String region) {
		this.region = region;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, initiator -> {
			regions.addAll(SavedRegions.get(game.level()).regions().compile().get(region));
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			// Add any players to the game
			List<ServerPlayer> players = game.level().players();
			for (ServerPlayer player : players) {
				// Already in the game?
				if (game.players().contains(player)) {
					continue;
				}

				for (BlockBox box : regions) {
					if (box.contains(player.position())) {
						game.players().add(player);
						game.invoker(GamePlayerEvents.ADD).onAdd(player);
						break;
					}
				}
			}

			for (ServerPlayer player : game.players()) {
				boolean keep = false;

				// Left world/game? always remove
				if (players.contains(player)) {
					for (BlockBox box : regions) {
						// Found the player in a region in the world
						if (box.contains(player.position())) {
							keep = true;
							break;
						}
					}
				}

				if (!keep) {
					game.players().remove(player);
					game.invoker(GamePlayerEvents.REMOVE).onRemove(player);
				}
			}
		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.GAME_REGION;
	}
}
