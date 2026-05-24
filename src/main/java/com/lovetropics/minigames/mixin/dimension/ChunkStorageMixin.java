package com.lovetropics.minigames.mixin.dimension;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// I am sorry for what I must do.
// We import our maps with new overworld dimension type settings, but the datafixer does not update the heights because it is not actually the overworld.
// Let's pretend to be the overworld. (Handled by ChunkHeightAndBiomeFix)
@Mixin(ChunkMap.class)
public class ChunkStorageMixin {
	// Todo 26.1 Port - Do we even this now?
//	@Redirect(method = "getChunkDataFixContextTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putString(Ljava/lang/String;Ljava/lang/String;)V"))
//	private static void injectDimensionContext(CompoundTag instance, String name, String value) {
//		if (value.startsWith(LoveTropics.ID + ":")) {
//			instance.putString(name, "minecraft:overworld");
//		} else {
//			instance.putString(name, value);
//		}
//	}
}
