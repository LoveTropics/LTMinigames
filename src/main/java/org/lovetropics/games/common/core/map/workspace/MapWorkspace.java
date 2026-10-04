package org.lovetropics.games.common.core.map.workspace;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.lovetropics.dimensions.RuntimeDimensionHandle;
import org.lovetropics.dimensions.RuntimeDimensions;
import org.lovetropics.dimensions.SharedDimensionState;
import org.lovetropics.games.common.core.map.MapMetadata;
import org.lovetropics.games.common.core.map.MapWorldSettings;

public record MapWorkspace(
		String id,
		WorkspaceDimensionConfig dimension,
		WorkspaceRegions regions,
		RuntimeDimensionHandle dimensionHandle
) {
	MapWorkspace(String id, WorkspaceDimensionConfig dimension, RuntimeDimensionHandle dimensionHandle) {
		this(id, dimension, new WorkspaceRegions(dimensionHandle.asKey(), false), dimensionHandle);
	}

	public ResourceKey<Level> dimensionKey() {
		return dimensionHandle.asKey();
	}

	public MapWorkspaceData intoData() {
		MapWorldSettings worldSettings = MapWorldSettings.copyOf(dimensionHandle.asLevel());
		return new MapWorkspaceData(id, dimension, worldSettings, regions.compile());
	}

	public void importFrom(MapMetadata metadata) {
		regions.importFrom(metadata.regions());
		ServerLevel level = dimensionHandle.asLevel();
		SharedDimensionState sharedState = RuntimeDimensions.get(level.getServer()).getSharedStateFor(level);
		if (sharedState != null) {
			metadata.settings().setupInto(level.getServer(), sharedState);
		}
	}
}
