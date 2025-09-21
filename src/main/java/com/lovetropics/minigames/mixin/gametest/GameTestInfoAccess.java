package com.lovetropics.minigames.mixin.gametest;

import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Collection;

@Mixin(GameTestInfo.class)
public interface GameTestInfoAccess {
	@Accessor
	Collection<GameTestListener> getListeners();
}
