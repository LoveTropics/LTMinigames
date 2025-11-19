package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class BeSpeedyState implements GameClientState {
	public static final BeSpeedyState INSTANCE = new BeSpeedyState();

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.BE_SPEEDY.get();
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		if (ClientGameStateManager.getOrNull(GameClientStateTypes.BE_SPEEDY) == null) {
			return;
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || player.gameMode() == GameType.SPECTATOR || player.gameMode() == GameType.CREATIVE) {
			return;
		}

		double movementY = Math.abs(player.getDeltaMovement().y);
		if (movementY < 0.1 && !player.isSteppingCarefully() && player.onGround()) {
			double factor = 1.35 - movementY * 0.2; // TODO config
			player.setDeltaMovement(player.getDeltaMovement().multiply(factor, 1.0, factor));
		}
	}
}
