package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.persistent.behavior.MakeSpeedyBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
		if (player == null) {
			return;
		}

		MakeSpeedyBehavior.applySpeed(player);
	}
}
