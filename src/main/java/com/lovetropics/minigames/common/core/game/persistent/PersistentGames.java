package com.lovetropics.minigames.common.core.game.persistent;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = LoveTropics.ID)
public class PersistentGames {
	private static final List<PersistentGameInstance> RUNNING_GAMES = new ArrayList<>();

	public static void start(MinecraftServer server, List<PersistentGameConfig> configs) {
		for (PersistentGameInstance game : RUNNING_GAMES) {
			game.invoker(GamePhaseEvents.STOP).stop(GameStopReason.reloading());
		}

		RUNNING_GAMES.clear();

		for (PersistentGameConfig config : configs) {
			ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, config.dimension()));
			if (level == null) {
				throw new IllegalStateException("Starting persistent game in non-existent world!");
			}

			PersistentGameInstance game = new PersistentGameInstance(server, level);

			for (PersistentBehaviorTemplate behavior : config.behaviors()) {
				behavior.instantiate().register(game, game.events());
			}

			game.invoker(GamePhaseEvents.CREATE).start();
			game.invoker(GamePhaseEvents.START).start();

			RUNNING_GAMES.add(game);
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
}
