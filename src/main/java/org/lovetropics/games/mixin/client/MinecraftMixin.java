package org.lovetropics.games.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.data.LoveTropicsAttachments;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.CollidersClientState;
import org.lovetropics.games.common.core.game.client_state.instance.TeamMembersClientState;
import org.lovetropics.games.common.core.game.client_state.instance.controls.RemapHotbarKeysClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Shadow
	public @Nullable LocalPlayer player;

	@Shadow
	public @Nullable HitResult hitResult;

	@Inject(at = @At("HEAD"), method = "shouldEntityAppearGlowing", cancellable = true)
	private void ltminigames$glowingTeamMembers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity.getType() == EntityTypes.PLAYER && ClientGameStateManager.getOrNull(GameClientStateTypes.GLOW_TEAM_MEMBERS) != null) {
			TeamMembersClientState team = ClientGameStateManager.getOrNull(GameClientStateTypes.TEAM_MEMBERS);

			if (team != null && (team.teamMembers().contains(entity.getUUID()) || player == entity)) {
				cir.setReturnValue(true);
			}
		}
		if (entity.hasData(LoveTropicsAttachments.HIGHLIGHT_COLOR)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "pick(F)V", at = @At("RETURN"))
	private void pick(float partialTicks, CallbackInfo ci) {
		HitResult hitResult = this.hitResult;
		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			return;
		}

		LocalPlayer player = this.player;
		CollidersClientState colliders = ClientGameStateManager.getOrNull(GameClientStateTypes.COLLIDERS);
		if (colliders == null || player == null) {
			return;
		}

		Vec3 start = player.getEyePosition(partialTicks);
		double currentDistance = hitResult.getLocation().distanceTo(start);

		Vec3 clip = colliders.clip(start, start.add(player.getViewVector(partialTicks).scale(currentDistance)));
		if (clip != null) {
			this.hitResult = BlockHitResult.miss(clip, Direction.UP, BlockPos.containing(clip));
		}
	}

	@Inject(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"), cancellable = true)
	public void onConsume(CallbackInfo ci, @Local(name = "i") int i) {
		RemapHotbarKeysClientState clientState = ClientGameStateManager.getOrNull(GameClientStateTypes.REMAP_QUICK_KEYS);
		if (clientState != null) {
			// Just to be safe, in theory this should never throw.
			try {
				int wantedSlot = clientState.getKeyFor(i);
				player.getInventory().setSelectedSlot(wantedSlot);
				ci.cancel();
			} catch (IndexOutOfBoundsException e) {
				return;
			}
		}
	}
}
