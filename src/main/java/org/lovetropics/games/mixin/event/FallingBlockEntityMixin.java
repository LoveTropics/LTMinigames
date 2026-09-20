package org.lovetropics.games.mixin.event;

import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity {

	public FallingBlockEntityMixin(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Shadow
	private BlockState blockState;

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;discard()V", ordinal = 2, shift = At.Shift.AFTER), method = "tick")
	private void customFalling(CallbackInfo ci) {
		if (level() instanceof ServerLevel serverLevel) {
			IGamePhase game = IGameLookup.get().getGamePhaseAt(serverLevel, blockPosition());
			if (game != null) {
				game.invoker(GameWorldEvents.BLOCK_LANDED).onBlockLanded(serverLevel, blockPosition(), blockState);
			}
		}
	}
}
