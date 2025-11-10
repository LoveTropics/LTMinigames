package com.lovetropics.minigames.common.core.game.persistent.behavior;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.BeSpeedyState;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class MakeSpeedyBehavior implements PersistentGameBehavior {
	public static final MapCodec<MakeSpeedyBehavior> CODEC = MapCodec.unit(MakeSpeedyBehavior::new);

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePlayerEvents.ADD, player -> {
			GameClientState.sendToPlayer(BeSpeedyState.INSTANCE, player);
		});

		events.listen(GamePlayerEvents.REMOVE, player -> {
			GameClientState.removeFromPlayer(GameClientStateTypes.BE_SPEEDY.get(), player);
		});

		events.listen(GamePlayerEvents.TICK, MakeSpeedyBehavior::applySpeed);
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.MAKE_SPEEDY;
	}


	public static void applySpeed(Player player) {
		double movementY = Math.abs(player.getDeltaMovement().y);
		if (movementY < 0.1 && !player.isSteppingCarefully()) {
			double factor = 1.35 - movementY * 0.2; // TODO config
			player.setDeltaMovement(player.getDeltaMovement().multiply(factor, 1.0, factor));
		}
	}
}
