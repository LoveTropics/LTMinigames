package com.lovetropics.minigames.common.core.map;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.map.workspace.MapWorkspaceManager;
import com.lovetropics.minigames.common.core.map.workspace.WorkspaceRegions;
import com.lovetropics.minigames.common.core.network.workspace.AddWorkspaceRegionMessage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

// WorkspaceRegions for persistent worlds
public class SavedRegions extends SavedData {
	public static final Codec<SavedRegions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			MapRegions.CODEC.fieldOf("regions").forGetter(SavedRegions::compile)
	).apply(instance, SavedRegions::new));

	private static final SavedDataType<SavedRegions> TYPE = new SavedDataType<>(
			LoveTropics.ID + "_saved_regions", SavedRegions::new, CODEC);

	private final WorkspaceRegions regions = new WorkspaceRegions(null);

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
