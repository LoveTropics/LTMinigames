package com.lovetropics.minigames.common.dev;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public record DevPackSource(Path root, PackType type, String packId, Component packName) implements RepositorySource {
	@Override
	public void loadPacks(Consumer<Pack> consumer) {
		PackLocationInfo locationInfo = new PackLocationInfo(packId, packName, PackSource.BUILT_IN, Optional.empty());
		PackSelectionConfig selectionConfig = new PackSelectionConfig(true, Pack.Position.TOP, false);
		Pack.ResourcesSupplier resourcesSupplier = new PathPackResources.PathResourcesSupplier(root);
		Pack pack = Pack.readMetaAndCreate(locationInfo, resourcesSupplier, type, selectionConfig);
		if (pack != null) {
			consumer.accept(pack);
		}
	}
}
