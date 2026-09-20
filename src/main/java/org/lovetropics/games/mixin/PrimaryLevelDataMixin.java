package org.lovetropics.games.mixin;

import org.lovetropics.games.common.util.duck.PrimaryLevelDataAccess;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(PrimaryLevelData.class)
public class PrimaryLevelDataMixin implements PrimaryLevelDataAccess {
	@Shadow
	@Final
	@Mutable
	private @Nullable UUID singlePlayerUUID;

	@Override
	public void ltminigames$setSinglePlayerUUID(UUID id) {
		singlePlayerUUID = id;
	}
}
