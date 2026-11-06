package org.lovetropics.games.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.client_state.GameClientStateSender;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.BouncyGroundClientState;
import org.lovetropics.games.common.core.game.client_state.instance.CollidersClientState;
import org.lovetropics.games.common.core.game.state.ColliderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {
	@Unique
	private double lt$landingFallDistance;

	@Inject(method = "collectCollidersIgnoringWorldBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getWorldBorder()Lnet/minecraft/world/level/border/WorldBorder;"))
	private static void collectColliders(Entity entity, Level level, List<VoxelShape> collisions, AABB boundingBox, CallbackInfoReturnable<List<VoxelShape>> cir, @Local ImmutableList.Builder<VoxelShape> output) {
		// Skip other entities for now, mostly to be cautious of performance
		if (!(entity instanceof Player player)) {
			return;
		}
		if (level.isClientSide()) {
			CollidersClientState colliders = ClientGameStateManager.getOrNull(GameClientStateTypes.COLLIDERS);
			if (colliders != null) {
				colliders.addTo(boundingBox, output);
			}
		} else {
			IGamePhase game = IGameLookup.get().getGamePhaseFor(player);
			if (game != null) {
				ColliderState colliderState = game.state().getOrNull(ColliderState.KEY);
				if (colliderState != null) {
					colliderState.getColliders(level.dimension()).addTo(boundingBox, output);
				}
			}
		}
	}

	@WrapOperation(method = "isLocalClientAuthoritative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isLocalClientAuthoritative()Z"))
	private boolean isLocalClientAuthoritative(LivingEntity passenger, Operation<Boolean> original) {
		if (passenger instanceof Player player && player.isLocalPlayer() && ClientGameStateManager.getOrNull(GameClientStateTypes.DISABLE_PLAYER_MOVEMENT) != null) {
			return false;
		}
		return original.call(passenger);
	}

	@Inject(method = "checkFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;resetFallDistance()V"))
	private void rememberLandingFallDistance(double ya, boolean onGround, BlockState onState, BlockPos pos, CallbackInfo ci) {
		lt$landingFallDistance = ((Entity) (Object) this).fallDistance;
	}

	// fall damage makes the server send the motion it has for the player which counters th bounce
	@WrapOperation(method = "checkFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;D)V"))
	private void keepBounceThroughFallDamage(Block block, Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance, Operation<Void> original) {
		boolean wasHurtMarked = entity.hurtMarked;
		original.call(block, level, state, pos, entity, fallDistance);
		if (entity instanceof ServerPlayer player) {
			BouncyGroundClientState bouncyGround = GameClientStateSender.get().byPlayer(player).getOrNull(GameClientStateTypes.BOUNCY_GROUND.get());
			if (bouncyGround != null && bouncyGround.bounces(player, fallDistance)) {
				entity.hurtMarked = wasHurtMarked;
			}
		}
	}

	@ModifyExpressionValue(method = "restituteMovementAfterCollisions(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZZLnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBlockBounciness(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)D"))
	private double bounceOffGround(double blockBounciness) {
		if ((Object) this instanceof Player player && player.isLocalPlayer()) {
			BouncyGroundClientState bouncyGround = ClientGameStateManager.getOrNull(GameClientStateTypes.BOUNCY_GROUND);
			if (bouncyGround != null && bouncyGround.bounces(player, lt$landingFallDistance)) {
				return Math.max(blockBounciness, bouncyGround.bounciness());
			}
		}
		return blockBounciness;
	}
}
