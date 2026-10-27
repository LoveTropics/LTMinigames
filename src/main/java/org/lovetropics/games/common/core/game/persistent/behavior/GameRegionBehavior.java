package org.lovetropics.games.common.core.game.persistent.behavior;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviorType;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;
import org.lovetropics.games.common.core.game.region.RegionQuery;

import java.util.List;
import java.util.function.Supplier;

public record GameRegionBehavior(
		RegionQuery region
) implements PersistentGameBehavior {
	public static final MapCodec<GameRegionBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
	        RegionQuery.CODEC.fieldOf("region").forGetter(b -> b.region)
	).apply(instance, GameRegionBehavior::new));

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePhaseEvents.TICK, () -> {
			// Add any players to the game
			List<ServerPlayer> players = game.level().players();
			for (ServerPlayer player : players) {
				// Already in the game?
				if (game.players().contains(player)) {
					continue;
				}

				if (region.contains(game, player)) {
					game.players().add(player);
					game.invoker(GamePlayerEvents.ADD).onAdd(player);
				}
			}

			for (ServerPlayer player : game.players()) {
				// Left world/game? always remove
				if (!region.contains(game, player)) {
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
