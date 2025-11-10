package com.lovetropics.minigames.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.content.turtle_race.RiderBehavior;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@WrapOperation(method = "handleSetEntityPassengersPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hasIndirectPassenger(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean disableDismountMessage(Entity entity, Entity player, Operation<Boolean> original) {
		return player.getExistingData(RiderBehavior.FORCE_RIDER).orElse(false) || original.call(entity, player);
	}
}
