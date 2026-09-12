package com.lovetropics.minigames.mixin.dimension;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.minigames.common.core.dimension.LinkedDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
	@WrapOperation(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;"))
	private ResourceKey<Level> getVanillaDimension(ServerLevel level, Operation<ResourceKey<Level>> original) {
		return LinkedDimensions.vanillaKey(level);
	}

	@WrapOperation(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getRespawnData()Lnet/minecraft/world/level/storage/LevelData$RespawnData;"))
	private LevelData.RespawnData getLinkedRespawnData(ServerLevel level, Operation<LevelData.RespawnData> original) {
		LinkedDimensions links = LinkedDimensions.get(level);
		return links != null ? links.respawnData() : original.call(level);
	}

	@WrapOperation(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
	private @Nullable ServerLevel getLinkedLevel(MinecraftServer server, ResourceKey<Level> dimension, Operation<ServerLevel> original, @Local(argsOnly = true) ServerLevel currentLevel) {
		ResourceKey<Level> linkedDimension = LinkedDimensions.resolve(currentLevel, dimension);
		return linkedDimension != null ? original.call(server, linkedDimension) : null;
	}

	// Players leave the End towards their own respawn point, which may be outside the group (e.g. the server's spawn) - keep them inside it
	@WrapOperation(method = "getPortalDestination", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"))
	private TeleportTransition keepRespawnInLinkedDimensions(ServerPlayer player, boolean consumeSpawnBlock, TeleportTransition.PostTeleportTransition postTeleportTransition, Operation<TeleportTransition> original, @Local(argsOnly = true) ServerLevel currentLevel) {
		TeleportTransition transition = original.call(player, consumeSpawnBlock, postTeleportTransition);
		LinkedDimensions links = LinkedDimensions.get(currentLevel);
		if (links == null || links.contains(transition.newLevel().dimension())) {
			return transition;
		}

		LevelData.RespawnData respawnData = links.respawnData();
		ServerLevel respawnLevel = currentLevel.getServer().getLevel(respawnData.dimension());
		if (respawnLevel == null) {
			return transition;
		}
		BlockPos pos = player.adjustSpawnLocation(respawnLevel, respawnData.pos());
		return new TeleportTransition(respawnLevel, Vec3.atBottomCenterOf(pos), Vec3.ZERO, respawnData.yaw(), respawnData.pitch(), postTeleportTransition);
	}
}
