package org.lovetropics.games.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.CollidersClientState;
import org.lovetropics.games.common.core.game.state.ColliderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public class EntityMixin {
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
					colliderState.colliders().addTo(boundingBox, output);
				}
			}
		}
	}
}
