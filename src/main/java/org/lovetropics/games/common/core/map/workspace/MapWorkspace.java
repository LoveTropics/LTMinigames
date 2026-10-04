package org.lovetropics.games.common.core.map.workspace;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.lovetropics.dimensions.RuntimeDimensionHandle;
import org.lovetropics.games.common.core.map.MapMetadata;
import org.lovetropics.games.common.core.map.MapWorldSettings;

public record MapWorkspace(
		String id,
		WorkspaceDimensionConfig dimension,
		MapWorldSettings worldSettings,
		WorkspaceRegions regions,
		RuntimeDimensionHandle dimensionHandle
) {
	MapWorkspace(String id, WorkspaceDimensionConfig dimension, MapWorldSettings worldSettings, RuntimeDimensionHandle dimensionHandle) {
		this(id, dimension, worldSettings, new WorkspaceRegions(dimensionHandle.asKey(), false), dimensionHandle);
	}

	public ResourceKey<Level> dimensionKey() {
		return dimensionHandle.asKey();
	}

	public MapWorkspaceData intoData() {
		return new MapWorkspaceData(id, dimension, worldSettings, regions.compile());
	}

	public void importFrom(MapMetadata metadata) {
		regions.importFrom(metadata.regions());
		worldSettings.importFrom(metadata.settings());
	}
}
