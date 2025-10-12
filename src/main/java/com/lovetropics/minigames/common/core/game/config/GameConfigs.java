package com.lovetropics.minigames.common.core.game.config;

import com.google.gson.JsonElement;
import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.util.DynamicTemplate;
import com.lovetropics.minigames.common.util.registry.RegistryLoadingOps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: Replace with a dynamic registry - currently blocked by not being able to /reload those
@EventBusSubscriber(modid = LoveTropics.ID)
public final class GameConfigs {
	private static final Logger LOGGER = LogManager.getLogger(GameConfigs.class);

	public static final CodecRegistry<ResourceLocation, GameConfig> REGISTRY = CodecRegistry.resourceLocationKeys();
	public static final CodecRegistry<ResourceLocation, GameBehaviorType<?>> CUSTOM_BEHAVIORS = CodecRegistry.resourceLocationKeys();

	private static final FileToIdConverter GAME_LISTER = FileToIdConverter.json("games");
	private static final FileToIdConverter BEHAVIOR_LISTER = FileToIdConverter.json("behaviors");

	@SubscribeEvent
	public static void addReloadListener(AddServerReloadListenersEvent event) {
		event.addListener(LoveTropics.location("game_configs"), new ContextAwareReloadListener() {
			@Override
			public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager, Executor backgroundExecutor, Executor gameExecutor) {
				return load(resourceManager, backgroundExecutor, getRegistryLookup())
						.thenCompose(barrier::wait)
						.thenAcceptAsync(configs -> {
							REGISTRY.clear();
							configs.stream()
									.sorted(Comparator.comparing(config -> config.name().getString()))
									.forEach(config -> REGISTRY.register(config.id(), config));
						}, gameExecutor);
			}
		});
	}

	private static CompletableFuture<List<GameConfig>> load(ResourceManager resourceManager, Executor backgroundExecutor, HolderLookup.Provider registryAccess) {
		return CompletableFuture.supplyAsync(() -> listBehaviors(resourceManager, backgroundExecutor), backgroundExecutor)
				.thenCompose(f -> f)
				.thenAccept(behaviors -> {
					CUSTOM_BEHAVIORS.clear();
					behaviors.forEach(CUSTOM_BEHAVIORS::register);
				})
				.thenComposeAsync(unused -> listConfigs(registryAccess, resourceManager, backgroundExecutor), backgroundExecutor);
	}

	private static CompletableFuture<List<GameConfig>> listConfigs(HolderLookup.Provider registryAccess, ResourceManager resourceManager, Executor executor) {
		DynamicOps<JsonElement> ops = RegistryLoadingOps.create(JsonOps.INSTANCE, registryAccess);
		List<CompletableFuture<GameConfig>> futures = GAME_LISTER.listMatchingResources(resourceManager).entrySet().stream()
				.map(entry -> CompletableFuture.supplyAsync(() -> tryLoadConfig(ops, entry.getKey(), entry.getValue()), executor))
				.toList();
		return Util.sequence(futures).thenApply(configs -> configs.stream().filter(Objects::nonNull).toList());
	}

	@Nullable
	private static GameConfig tryLoadConfig(DynamicOps<JsonElement> ops, ResourceLocation path, Resource resource) {
		try {
			// TODO: Mark games that had only a partial result with a warning in the UI?
			return loadConfig(ops, path, resource)
					.resultOrPartial(error -> LOGGER.error("Failed to load game config at {}: {}", path, error))
					.orElse(null);
		} catch (Exception e) {
			LOGGER.error("Failed to load game config at {}", path, e);
			return null;
		}
	}

	private static DataResult<GameConfig> loadConfig(DynamicOps<JsonElement> ops, ResourceLocation path, Resource resource) throws IOException {
		try (BufferedReader reader = resource.openAsReader()) {
			JsonElement json = StrictJsonParser.parse(reader);
			Codec<GameConfig> codec = GameConfig.codec(GAME_LISTER.fileToId(path));
			return codec.parse(ops, json);
		}
	}

	private static CompletableFuture<Map<ResourceLocation, GameBehaviorType<?>>> listBehaviors(ResourceManager resourceManager, Executor executor) {
		List<CompletableFuture<Map.Entry<ResourceLocation, GameBehaviorType<?>>>> futures = BEHAVIOR_LISTER.listMatchingResources(resourceManager).entrySet().stream()
				.map(entry -> CompletableFuture.supplyAsync(() -> tryLoadBehavior(entry.getValue(), entry.getKey()), executor))
				.toList();
		return Util.sequence(futures).thenApply(behaviors -> behaviors.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
		);
	}

	@Nullable
	private static Map.Entry<ResourceLocation, GameBehaviorType<?>> tryLoadBehavior(Resource resource, ResourceLocation path) {
		try {
			try (BufferedReader reader = resource.openAsReader()) {
				JsonElement json = StrictJsonParser.parse(reader);
				ResourceLocation id = BEHAVIOR_LISTER.fileToId(path);
				return Map.entry(id, new GameBehaviorType<>(createCustomBehaviorCodec(DynamicTemplate.parse(JsonOps.INSTANCE, json))));
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load custom behavior at {}", path, e);
			return null;
		}
	}

	private static MapCodec<IGameBehavior> createCustomBehaviorCodec(final DynamicTemplate template) {
		return new MapCodec<>() {
			@Override
			public <T> RecordBuilder<T> encode(IGameBehavior input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				final DataResult<T> substituted = IGameBehavior.CODEC.encodeStart(ops, input);
				if (substituted.result().isEmpty()) {
					return prefix.withErrorsFrom(substituted);
				}
				final T extracted = template.extract(ops, substituted.result().get());
				final MutableObject<RecordBuilder<T>> builder = new MutableObject<>(prefix);
				ops.getMap(extracted).result().ifPresent(map ->
						map.entries().forEach(pair ->
								builder.setValue(builder.getValue().add(pair.getFirst(), pair.getSecond()))
						)
				);
				return builder.getValue();
			}

			@Override
			public <T> DataResult<IGameBehavior> decode(DynamicOps<T> ops, MapLike<T> input) {
				T substituted = template.substituteMap(ops, input);
				return IGameBehavior.CODEC.parse(ops, substituted);
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return template.parameters().stream().map(path -> path.segments()[0]).distinct().map(ops::createString);
			}
		};
	}
}
