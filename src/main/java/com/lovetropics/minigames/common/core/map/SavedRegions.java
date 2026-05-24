package com.lovetropics.minigames.common.core.map;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.map.workspace.WorkspaceRegions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

// WorkspaceRegions for persistent worlds
public class SavedRegions extends SavedData {
	public static final Identifier ID = LoveTropics.location("saved_regions");

	public static final Codec<SavedRegions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			MapRegions.CODEC.fieldOf("regions").forGetter(SavedRegions::compile)
	).apply(instance, SavedRegions::new));

	private static final SavedDataType<SavedRegions> TYPE = new SavedDataType<>(ID, SavedRegions::new, CODEC);

	private final WorkspaceRegions regions = new WorkspaceRegions(null, true);

	public static SavedRegions get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public SavedRegions() {

	}

	public SavedRegions(MapRegions regions) {
		this.regions.importFrom(regions);
	}

	public MapRegions compile() {
		return regions.compile();
	}

	public WorkspaceRegions regions() {
		return regions;
	}

	@Override
	public boolean isDirty() {
		// Just re-save always for now, alternative requires some sort of markDirty hook in WorkspaceRegions
		return true;
	}
}
