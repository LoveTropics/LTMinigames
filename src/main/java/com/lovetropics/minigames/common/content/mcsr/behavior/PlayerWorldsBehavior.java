package com.lovetropics.minigames.common.content.mcsr.behavior;

import com.lovetropics.minigames.common.content.mcsr.McsrTexts;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PendingSubPhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.config.GamePhaseConfig;
import com.lovetropics.minigames.common.core.game.map.GeneratorMapProvider;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/// Gives every participant their own copy of the same generated world, each running as a sub-phase of this game.
/// The world config must use a generator map: its seed is replaced with one shared by every player of this game.
public record PlayerWorldsBehavior(Identifier world, Optional<Long> seed) implements IGameBehavior {
	public static final MapCodec<PlayerWorldsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.fieldOf("world").forGetter(PlayerWorldsBehavior::world),
			Codec.LONG.optionalFieldOf("seed").forGetter(PlayerWorldsBehavior::seed)
	).apply(i, PlayerWorldsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		IGameDefinition worldConfig = createWorldConfig(game);
		Map<UUID, PlayerWorld> worlds = new HashMap<>();

		events.listen(GamePhaseEvents.START, initiator -> {
			for (ServerPlayer player : game.participants()) {
				PlayerWorld world = new PlayerWorld(game, worldConfig);
				worlds.put(player.getUUID(), world);
				player.sendSystemMessage(McsrTexts.PREPARING_WORLD);
				world.sendPlayer(player);
			}
		});

		// Players rejoining after a disconnect go straight back into their own world
		events.listen(GamePlayerEvents.JOIN, player -> {
			PlayerWorld world = worlds.get(player.getUUID());
			if (world != null) {
				world.sendPlayer(player);
			}
		});
	}

	private IGameDefinition createWorldConfig(IGamePhase game) {
		GameConfig config = GameConfigs.REGISTRY.get(world);
		if (config == null) {
			throw new GameException(Component.literal("No game config with id: " + world));
		}
		if (!(config.playing().map() instanceof GeneratorMapProvider generator)) {
			throw new GameException(Component.literal("Player world config '" + world + "' must use a generator map"));
		}
		long seed = this.seed.orElseGet(() -> game.random().nextLong());
		return config.withPlayingPhase(new GamePhaseConfig(generator.withSeed(seed), config.playing().behaviors()));
	}

	private static final class PlayerWorld {
		private final IGamePhase topGame;
		private final PendingSubPhase pendingPhase;
		private @Nullable IGamePhase phase;
		private boolean closed;

		private PlayerWorld(IGamePhase topGame, IGameDefinition config) {
			this.topGame = topGame;
			pendingPhase = topGame.createSubPhase(config);
			pendingPhase.whenCreated((subGame, subEvents) -> {
				phase = subGame;
				subEvents.listen(GamePhaseEvents.STOP, reason -> closed = true);
			});
			pendingPhase.whenErrored(exception -> {
				closed = true;
				topGame.requestStop(GameStopReason.errored(Component.literal("Failed to create player world: " + exception.getMessage())));
			});
		}

		public void sendPlayer(ServerPlayer player) {
			if (closed) {
				return;
			}
			if (phase != null) {
				topGame.transferPlayerTo(player, phase);
			} else {
				pendingPhase.queuePlayer(player);
			}
		}
	}
}
