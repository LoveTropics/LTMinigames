package org.lovetropics.games.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.lovetropics.games.client.game.ClientGameStateManager;
import org.lovetropics.games.common.core.game.client_state.GameClientStateSender;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TridentItem.class)
public class TridentItemMixin {
	@WrapOperation(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;push(DDD)V"))
	private void pushRiptide(Player player, double dx, double dy, double dz, Operation<Void> original) {
		// TODO: This is so unbelievably stupid, I am sorry
		boolean reversed;
		if (player instanceof ServerPlayer serverPlayer) {
			reversed = GameClientStateSender.get().byPlayer(serverPlayer).getOrNull(GameClientStateTypes.REVERSE_RIPTIDE.get()) != null;
		} else {
			reversed = ClientGameStateManager.isSet(GameClientStateTypes.REVERSE_RIPTIDE);
		}
		if (reversed) {
			original.call(player, -dx, -dy, -dz);
		} else {
			original.call(player, dx, dy, dz);
		}
	}
}
