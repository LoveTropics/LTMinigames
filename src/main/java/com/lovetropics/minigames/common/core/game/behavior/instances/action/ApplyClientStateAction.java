package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record ApplyClientStateAction(GameClientState state) implements IGameBehavior {
	public static final MapCodec<ApplyClientStateAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameClientState.CODEC.fieldOf("state").forGetter(ApplyClientStateAction::state)
	).apply(i, ApplyClientStateAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		Set<UUID> appliedToPlayers = new HashSet<>();
		events.applyToPlayers(game, (context, target) -> {
			GameClientState.sendToPlayer(state, target);
			appliedToPlayers.add(target.getUUID());
			return true;
		});
		events.listen(GamePlayerEvents.REMOVE, player -> {
			if (appliedToPlayers.remove(player.getUUID())) {
				GameClientState.removeFromPlayer(state.getType(), player);
			}
		});
	}
}
