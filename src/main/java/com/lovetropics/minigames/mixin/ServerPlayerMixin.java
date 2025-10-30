package com.lovetropics.minigames.mixin;

import com.lovetropics.minigames.common.core.game.impl.GameEventDispatcher;
import com.lovetropics.minigames.common.util.duck.ServerPlayerExtension;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements ServerPlayerExtension {
	private ServerPlayerMixin(Level level, GameProfile gameProfile) {
		super(level, gameProfile);
	}

	@Inject(method = "initMenu", at = @At("HEAD"))
	private void initMenu(AbstractContainerMenu menu, CallbackInfo ci) {
		menu.addSlotListener(GameEventDispatcher.instance.createInventoryListener((ServerPlayer) (Object) this));
	}

	@Unique
	private int tabListOrder = 0;

	@Inject(method = "getTabListOrder", at = @At("HEAD"))
	private void modifyTabListOrder(CallbackInfoReturnable<Integer> cir) {
		if (tabListOrder != 0) {
			cir.setReturnValue(tabListOrder);
		}
	}

	@Override
	public void lt$setPlayerListOrder(int order) {
		tabListOrder = order;
		syncChange();
	}

	@Unique
	private void syncChange() {
		getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER, (ServerPlayer) (Object) this));
	}
}
