package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Nullable
	@Shadow
	public LocalPlayer player;

	@Inject(at = @At("HEAD"), method = "shouldEntityAppearGlowing", cancellable = true)
	private void ltminigames$glowingTeamMembers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity.getType() == EntityType.PLAYER && ClientGameStateManager.getOrNull(GameClientStateTypes.GLOW_TEAM_MEMBERS) != null) {
			final var team = ClientGameStateManager.getOrNull(GameClientStateTypes.TEAM_MEMBERS);

			if (team != null && (team.teamMembers().contains(entity.getUUID()) || player == entity)) {
				cir.setReturnValue(true);
			}
		}
	}
}
