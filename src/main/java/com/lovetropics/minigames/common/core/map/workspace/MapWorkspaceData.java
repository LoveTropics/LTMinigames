package com.lovetropics.minigames.common.core.map.workspace;

import com.lovetropics.minigames.common.core.dimension.RuntimeDimensionHandle;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.map.MapWorldSettings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MapWorkspaceData(
		String id,
		WorkspaceDimensionConfig dimension,
		MapWorldSettings worldSettings,
		MapRegions regions
) {
	public static final Codec<MapWorkspaceData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(c -> c.id),
			WorkspaceDimensionConfig.CODEC.fieldOf("dimension").forGetter(c -> c.dimension),
			MapWorldSettings.CODEC.fieldOf("settings").forGetter(c -> c.worldSettings),
			MapRegions.CODEC.fieldOf("regions").forGetter(c -> c.regions)
	).apply(i, MapWorkspaceData::new));

	public MapWorkspace create(RuntimeDimensionHandle dimensionHandle) {
		WorkspaceRegions regions = new WorkspaceRegions(dimensionHandle.asKey(), false);
		regions.importFrom(this.regions);

		return new MapWorkspace(id, dimension, worldSettings, regions, dimensionHandle);
	}
}
