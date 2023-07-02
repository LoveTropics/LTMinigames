package com.lovetropics.minigames.common.core.game.weather;

import com.lovetropics.minigames.Constants;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = Constants.MODID)
public final class WeatherControllerManager {
	private static final Map<ResourceKey<Level>, WeatherController> WEATHER_CONTROLLERS = new Reference2ObjectOpenHashMap<>();

	private static Function<ServerLevel, WeatherController> factory = VanillaWeatherController::new;

	public static void setFactory(Function<ServerLevel, WeatherController> factory) {
		WeatherControllerManager.factory = factory;
	}

	public static WeatherController forWorld(ServerLevel world) {
		ResourceKey<Level> dimension = world.dimension();
		WeatherController controller = WEATHER_CONTROLLERS.get(dimension);
		if (controller == null) {
			WEATHER_CONTROLLERS.put(dimension, controller = factory.apply(world));
		}
		return controller;
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		joinPlayerToDimension(event.getEntity(), event.getEntity().level().dimension());
	}

	@SubscribeEvent
	public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		joinPlayerToDimension(event.getEntity(), event.getTo());
	}

	private static void joinPlayerToDimension(Player player, ResourceKey<Level> dimension) {
		MinecraftServer server = player.getServer();
		if (server == null || !(player instanceof ServerPlayer)) {
			return;
		}

		ServerLevel world = server.getLevel(dimension);
		if (world != null) {
			WeatherController controller = WeatherControllerManager.forWorld(world);
			controller.onPlayerJoin((ServerPlayer) player);
		}
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		Level level = event.level;
		if (level.isClientSide || event.phase == TickEvent.Phase.END) {
			return;
		}

		WeatherController controller = WEATHER_CONTROLLERS.get(level.dimension());
		if (controller != null) {
			controller.tick();
		}
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevelAccessor level) {
			WEATHER_CONTROLLERS.remove(level.getLevel().dimension());
		}
	}
}
