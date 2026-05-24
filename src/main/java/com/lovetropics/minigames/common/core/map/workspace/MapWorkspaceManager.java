package com.lovetropics.minigames.common.core.map.workspace;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionHandle;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.lovetropics.minigames.common.core.map.MapWorldInfo;
import com.lovetropics.minigames.common.core.map.MapWorldSettings;
import com.lovetropics.minigames.common.core.map.SavedRegions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class MapWorkspaceManager extends SavedData {
	public static final Identifier ID = LoveTropics.id("map_workspace_manager");

	private static final SavedDataType<MapWorkspaceManager> TYPE = new SavedDataType<>(
			ID,
			context -> new MapWorkspaceManager(context.getServer()),
			context -> Packed.CODEC.xmap(
					packed -> MapWorkspaceManager.load(context.getServer(), packed),
					MapWorkspaceManager::pack
			)
	);

	private final MinecraftServer server;
	private final Map<String, MapWorkspace> workspaces = new Object2ObjectOpenHashMap<>();

	private MapWorkspaceManager(MinecraftServer server) {
		this.server = server;
	}

	public static MapWorkspaceManager get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public CompletableFuture<MapWorkspace> openWorkspace(String id, WorkspaceDimensionConfig dimensionConfig) {
		MapWorldSettings settings = MapWorldSettings.createFromOverworld(server);

		return CompletableFuture.supplyAsync(() -> getOrCreateDimension(id, dimensionConfig, settings), server)
				.thenApplyAsync(dimensionHandle -> {
					MapWorkspace workspace = new MapWorkspace(id, dimensionConfig, settings, dimensionHandle);
					workspaces.putIfAbsent(id, workspace);

					return workspace;
				}, server);
	}

	private RuntimeDimensionHandle getOrCreateDimension(String id, WorkspaceDimensionConfig dimensionConfig, MapWorldSettings mapSettings) {
		return RuntimeDimensions.get(server).getOrOpenPersistent(LoveTropics.id(id), () -> {
			MapWorldInfo worldInfo = MapWorldInfo.create(server, mapSettings);
			return dimensionConfig.toRuntimeConfig(worldInfo);
		});
	}

	public boolean deleteWorkspace(String id) {
		MapWorkspace workspace = workspaces.remove(id);
		if (workspace != null) {
			workspace.dimensionHandle().delete();
			return true;
		}
		return false;
	}

	@Nullable
	public MapWorkspace getWorkspace(String id) {
		return workspaces.get(id);
	}

	@Nullable
	public MapWorkspace getWorkspace(ResourceKey<Level> dimension) {
		Identifier name = dimension.identifier();
		if (!name.getNamespace().equals(LoveTropics.ID)) {
			return null;
		}
		return workspaces.get(name.getPath());
	}

	@Nullable
	public WorkspaceRegions getRegions(MinecraftServer server, ResourceKey<Level> dimension) {
		MapWorkspace workspace = getWorkspace(dimension);
		if (workspace != null) {
			return workspace.regions();
		}

		ServerLevel level = server.getLevel(dimension);
		if (level != null) {
			return SavedRegions.get(level).regions();
		}

		return null;
	}

	public Set<String> getWorkspaceIds() {
		return workspaces.keySet();
	}

	public boolean hasWorkspace(String id) {
		return workspaces.containsKey(id);
	}

	public boolean isWorkspace(ResourceKey<Level> dimension) {
		return getWorkspace(dimension) != null;
	}

	private Packed pack() {
		return new Packed(workspaces.values().stream().map(MapWorkspace::intoData).toList());
	}

	private static MapWorkspaceManager load(MinecraftServer server, Packed packed) {
		MapWorkspaceManager manager = new MapWorkspaceManager(server);
		for (MapWorkspaceData workspaceData : packed.workspaces) {
			RuntimeDimensionHandle dimensionHandle = manager.getOrCreateDimension(workspaceData.id(), workspaceData.dimension(), workspaceData.worldSettings());
			MapWorkspace workspace = workspaceData.create(dimensionHandle);
			manager.workspaces.put(workspaceData.id(), workspace);
		}
		return manager;
	}

	@Override
	public boolean isDirty() {
		return true;
	}

	private record Packed(
			List<MapWorkspaceData> workspaces
	) {
		public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group(
				MapWorkspaceData.CODEC.listOf().fieldOf("workspaces").forGetter(Packed::workspaces)
		).apply(i, Packed::new));
	}
}
