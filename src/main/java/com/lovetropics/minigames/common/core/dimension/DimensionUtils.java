package com.lovetropics.minigames.common.core.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Set;

public class DimensionUtils {
    public static void teleportPlayerNoPortal(ServerPlayer player, ResourceKey<Level> destination, BlockPos pos) {
		ServerLevel level = player.level().getServer().getLevel(destination);
		player.teleportTo(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
	}

	public static Holder<DimensionType> overworld(MinecraftServer server) {
    	return server.overworld().dimensionTypeRegistration();
	}
}
