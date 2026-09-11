package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.network.ClientboundPlayerFaceDVDPackets;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Supplier;

public record ClearPlayerDVDFaceAction() implements IGameBehavior {

	public static final MapCodec<ClearPlayerDVDFaceAction> CODEC = MapCodec.unit(ClearPlayerDVDFaceAction::new);


	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (contextMap, player) -> {
			PacketDistributor.sendToPlayer(player, new ClientboundPlayerFaceDVDPackets.Clear());
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.CLEAR_DVD_FACE;
	}
}
