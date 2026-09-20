package org.lovetropics.games.common.core.game.behavior.instances;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.network.ClientboundFadeToBlackPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.neoforged.neoforge.network.PacketDistributor;

public record FadeToBlackBehaviour(
		int fadeDuration
) implements IGameBehavior {

	public static final MapCodec<FadeToBlackBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("fade", SharedConstants.TICKS_PER_SECOND).forGetter(FadeToBlackBehaviour::fadeDuration)
	).apply(i, FadeToBlackBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (contextMap, player) -> {
			PacketDistributor.sendToPlayer(player, new ClientboundFadeToBlackPacket(true, fadeDuration));
			return true;
		});
	}
}
