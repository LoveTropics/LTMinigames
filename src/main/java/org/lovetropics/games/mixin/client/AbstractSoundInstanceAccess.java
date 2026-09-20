package org.lovetropics.games.mixin.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccess {
	@Accessor("volume")
	void setBaseVolume(float volume);

	@Accessor("volume")
	float getBaseVolume();
}
