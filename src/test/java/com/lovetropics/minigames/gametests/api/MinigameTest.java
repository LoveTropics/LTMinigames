package com.lovetropics.minigames.gametests.api;

import com.lovetropics.minigames.common.core.game.datagen.BehaviorFactory;
import com.lovetropics.minigames.common.core.game.datagen.GameProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

public interface MinigameTest {
	void generateGame(GameProvider.GameGenerator generator, BehaviorFactory behaviors, HolderLookup.Provider registries);

	ResourceLocation id();

	default ResourceLocation gameId(String... path) {
		return id().withSuffix("/" + String.join("/", path));
	}
}
