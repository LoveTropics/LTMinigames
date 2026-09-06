package com.lovetropics.minigames.common.core.game.persistent;

import com.google.gson.JsonElement;
import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.DynamicTemplate;
import com.lovetropics.minigames.common.util.registry.RegistryLoadingOps;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.util.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.resource.ContextAwareReloadListener;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EventBusSubscriber(modid = LoveTropics.ID)
public class PersistentGameConfigs {
	private static final Logger LOGGER = LogManager.getLogger(PersistentGameConfigs.class);

	public static final CodecRegistry<Identifier, PersistentGameConfig> REGISTRY = CodecRegistry.idKeys();
	public static final CodecRegistry<Identifier, PersistentGameBehaviorType<?>> CUSTOM_BEHAVIORS = CodecRegistry.idKeys();

	private static final FileToIdConverter GAME_LISTER = FileToIdConverter.json("persistent_games");
	private static final FileToIdConverter BEHAVIOR_LISTER = FileToIdConverter.json("persistent_behaviors");
	private static final AtomicBoolean RELOAD_GAMES = new AtomicBoolean(true);

	@SubscribeEvent
	public static void addReloadListener(AddServerReloadListenersEvent event) {
		event.addListener(LoveTropics.id("persistent_game_configs"), new ContextAwareReloadListener() {
			@Override
			public CompletableFuture<Void> reload(SharedState currentReload, Executor taskExecutor, PreparationBarrier preparationBarrier, Executor reloadExecutor) {
				return load(currentReload.resourceManager(), taskExecutor, getRegistryLookup())
						.thenCompose(preparationBarrier::wait)
						.thenAcceptAsync(configs -> {
							REGISTRY.clear();
							configs.stream()
									.forEach(config -> REGISTRY.register(config.id(), config));

							// Mark needing a reload on the main tick loop
							RELOAD_GAMES.set(true);

						}, reloadExecutor);
			}
		});
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Pre event) {
		if (RELOAD_GAMES.get()) {
			RELOAD_GAMES.set(false);

			PersistentGames.start(event.getServer(), REGISTRY.stream().toList());
		}
	}

	private static CompletableFuture<List<PersistentGameConfig>> load(ResourceManager resourceManager, Executor backgroundExecutor, HolderLookup.Provider registryAccess) {
		return CompletableFuture.supplyAsync(() -> listBehaviors(resourceManager, backgroundExecutor), backgroundExecutor)
				.thenCompose(f -> f)
				.thenAccept(behaviors -> {
					CUSTOM_BEHAVIORS.clear();
					behaviors.forEach(CUSTOM_BEHAVIORS::register);
				})
				.thenComposeAsync(unused -> listConfigs(registryAccess, resourceManager, backgroundExecutor), backgroundExecutor);
	}

	private static CompletableFuture<List<PersistentGameConfig>> listConfigs(HolderLookup.Provider registryAccess, ResourceManager resourceManager, Executor executor) {
		DynamicOps<JsonElement> ops = RegistryLoadingOps.create(JsonOps.INSTANCE, registryAccess);
		List<CompletableFuture<PersistentGameConfig>> futures = GAME_LISTER.listMatchingResources(resourceManager).entrySet().stream()
				.map(entry -> CompletableFuture.supplyAsync(() -> tryLoadConfig(ops, entry.getKey(), entry.getValue()), executor))
				.toList();
		return Util.sequence(futures).thenApply(configs -> configs.stream().filter(Objects::nonNull).toList());
	}

	private static @Nullable PersistentGameConfig tryLoadConfig(DynamicOps<JsonElement> ops, Identifier path, Resource resource) {
		try {
			return loadConfig(ops, path, resource)
					.resultOrPartial(error -> LOGGER.error("Failed to load persistent game config at {}: {}", path, error))
					.orElse(null);
		} catch (Exception e) {
			LOGGER.error("Failed to load persistent game config at {}", path, e);
			return null;
		}
	}

	private static DataResult<PersistentGameConfig> loadConfig(DynamicOps<JsonElement> ops, Identifier path, Resource resource) throws IOException {
		try (BufferedReader reader = resource.openAsReader()) {
			JsonElement json = StrictJsonParser.parse(reader);
			return PersistentGameConfig.codec(GAME_LISTER.fileToId(path)).parse(ops, json);
		}
	}

	private static CompletableFuture<Map<Identifier, PersistentGameBehaviorType<?>>> listBehaviors(ResourceManager resourceManager, Executor executor) {
		List<CompletableFuture<Map.Entry<Identifier, PersistentGameBehaviorType<?>>>> futures = BEHAVIOR_LISTER.listMatchingResources(resourceManager).entrySet().stream()
				.map(entry -> CompletableFuture.supplyAsync(() -> tryLoadBehavior(entry.getValue(), entry.getKey()), executor))
				.toList();
		return Util.sequence(futures).thenApply(behaviors -> behaviors.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
		);
	}

	private static Map.Entry<Identifier, @Nullable PersistentGameBehaviorType<?>> tryLoadBehavior(Resource resource, Identifier path) {
		try {
			try (BufferedReader reader = resource.openAsReader()) {
				JsonElement json = StrictJsonParser.parse(reader);
				Identifier id = BEHAVIOR_LISTER.fileToId(path);
				return Map.entry(id, new PersistentGameBehaviorType<>(createCustomBehaviorCodec(DynamicTemplate.parse(JsonOps.INSTANCE, json))));
			}
		} catch (Exception e) {
			LOGGER.error("Failed to load custom behavior at {}", path, e);
			return null;
		}
	}

	private static MapCodec<PersistentGameBehavior> createCustomBehaviorCodec(final DynamicTemplate template) {
		return new MapCodec<>() {
			@Override
			public <T> RecordBuilder<T> encode(PersistentGameBehavior input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				final DataResult<T> substituted = PersistentGameBehavior.CODEC.encodeStart(ops, input);
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
			public <T> DataResult<PersistentGameBehavior> decode(DynamicOps<T> ops, MapLike<T> input) {
				T substituted = template.substituteMap(ops, input);
				return PersistentGameBehavior.CODEC.parse(ops, substituted);
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return template.parameters().stream().map(path -> path.segments()[0]).distinct().map(ops::createString);
			}
		};
	}

}
