package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.google.gson.JsonElement;
import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.RegistryLoadingOps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@EventBusSubscriber(modid = LoveTropics.ID)
public class DDRMachineLevels {
	private static final Logger LOGGER = LogManager.getLogger(DDRMachineLevels.class);

	public static final CodecRegistry<ResourceLocation, DDRMachineLevel> REGISTRY = CodecRegistry.resourceLocationKeys();

	private static final FileToIdConverter LEVEL_LISTER = FileToIdConverter.json("ddr_levels");

	@SubscribeEvent
	public static void addReloadListener(AddServerReloadListenersEvent event) {
		event.addListener(LoveTropics.location("ddr_levels"), new ContextAwareReloadListener() {
			@Override
			public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager, Executor backgroundExecutor, Executor gameExecutor) {
				return load(resourceManager, backgroundExecutor, getRegistryLookup())
						.thenCompose(barrier::wait)
						.thenAcceptAsync(levels -> {
							REGISTRY.clear();
							levels.stream()
									.sorted(Comparator.comparing(level -> level.name()))
									.forEach(level -> REGISTRY.register(level.id(), level));
						}, gameExecutor);
			}
		});
	}

	private static CompletableFuture<List<DDRMachineLevel>> load(ResourceManager resourceManager, Executor backgroundExecutor, HolderLookup.Provider registryAccess) {
		return CompletableFuture.supplyAsync(() -> listLevels(registryAccess, resourceManager, backgroundExecutor), backgroundExecutor)
				.thenCompose(f -> f);
	}

	private static CompletableFuture<List<DDRMachineLevel>> listLevels(HolderLookup.Provider registryAccess, ResourceManager resourceManager, Executor executor) {
		DynamicOps<JsonElement> ops = RegistryLoadingOps.create(JsonOps.INSTANCE, registryAccess);
		List<CompletableFuture<DDRMachineLevel>> futures = LEVEL_LISTER.listMatchingResources(resourceManager).entrySet().stream()
				.map(entry -> CompletableFuture.supplyAsync(() -> tryLoadLevel(ops, entry.getKey(), entry.getValue()), executor))
				.toList();
		return Util.sequence(futures).thenApply(configs -> configs.stream().filter(Objects::nonNull).toList());
	}

	@Nullable
	private static DDRMachineLevel tryLoadLevel(DynamicOps<JsonElement> ops, ResourceLocation path, Resource resource) {
		try {
			return loadLevel(ops, path, resource)
					.resultOrPartial(error -> LOGGER.error("Failed to load ddr level at {}: {}", path, error))
					.orElse(null);
		} catch (Exception e) {
			LOGGER.error("Failed to load ddr level at {}", path, e);
			return null;
		}
	}

	private static DataResult<DDRMachineLevel> loadLevel(DynamicOps<JsonElement> ops, ResourceLocation path, Resource resource) throws IOException {
		try (BufferedReader reader = resource.openAsReader()) {
			JsonElement json = StrictJsonParser.parse(reader);
			Codec<DDRMachineLevel> codec = DDRMachineLevel.codec(LEVEL_LISTER.fileToId(path));
			return codec.parse(ops, json);
		}
	}
}
