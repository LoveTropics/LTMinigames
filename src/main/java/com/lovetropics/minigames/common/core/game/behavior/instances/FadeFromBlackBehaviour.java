package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.network.ClientboundFadeToBlackPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.neoforged.neoforge.network.PacketDistributor;

public record FadeFromBlackBehaviour(
		int fadeDuration
) implements IGameBehavior {

	public static final MapCodec<FadeFromBlackBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("fade", SharedConstants.TICKS_PER_SECOND).forGetter(FadeFromBlackBehaviour::fadeDuration)
	).apply(i, FadeFromBlackBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (contextMap, player) -> {
			PacketDistributor.sendToPlayer(player, new ClientboundFadeToBlackPacket(false, fadeDuration));
			return true;
		});
	}
}
