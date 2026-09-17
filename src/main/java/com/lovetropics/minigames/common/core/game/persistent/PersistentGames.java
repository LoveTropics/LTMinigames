package com.lovetropics.minigames.common.core.game.persistent;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = LoveTropics.ID)
public class PersistentGames {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final List<PersistentGameInstance> RUNNING_GAMES = new ArrayList<>();
	private static final Map<ResourceKey<Level>, List<PersistentGameInstance>> BY_LEVEL = new HashMap<>();

	public static void start(MinecraftServer server, List<PersistentGameConfig> configs) {
		for (PersistentGameInstance game : RUNNING_GAMES) {
			game.invoker(GamePhaseEvents.STOP).stop(GameStopReason.reloading());
		}

		BY_LEVEL.clear();
		RUNNING_GAMES.clear();

		for (PersistentGameConfig config : configs) {
			ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, config.dimension());
			ServerLevel level = server.getLevel(key);
			if (level == null) {
				LOGGER.error("Failed to start persistent game {} in non-existent world {}!", config.id(), config.dimension());
				continue;
			}

			PersistentGameInstance game = new PersistentGameInstance(server, level);

			for (PersistentBehaviorTemplate behavior : config.behaviors()) {
				behavior.instantiate().register(game, game.events());
			}

			game.invoker(GamePhaseEvents.CREATE).create();
			game.invoker(GamePhaseEvents.START).start(null);

			RUNNING_GAMES.add(game);
			BY_LEVEL.computeIfAbsent(key, k -> new ArrayList<>()).add(game);
		}
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		for (PersistentGameInstance game : new ArrayList<>(RUNNING_GAMES)) {
			try {
				game.invoker(GamePhaseEvents.TICK).tick();
			} catch (Exception e) {
				e.printStackTrace();
				game.invoker(GamePhaseEvents.STOP).stop(GameStopReason.errored(Component.literal("Error in tick: " + e.getMessage())));
				RUNNING_GAMES.remove(game);
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		for (PersistentGameInstance game : new ArrayList<>(RUNNING_GAMES)) {
			game.invoker(GamePhaseEvents.STOP).stop(GameStopReason.serverStopping());
		}
	}

	public static List<PersistentGameInstance> in(Level level) {
		return BY_LEVEL.getOrDefault(level.dimension(), List.of());
	}
}
