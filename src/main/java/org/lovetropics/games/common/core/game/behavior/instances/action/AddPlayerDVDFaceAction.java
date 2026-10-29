package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.network.ClientboundPlayerFaceDVDPackets;

import java.util.UUID;

public record AddPlayerDVDFaceAction(UUID uuid, int lengthInTicks) implements IGameBehavior {

	public static final MapCodec<AddPlayerDVDFaceAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			UUIDUtil.CODEC.fieldOf("uuid").forGetter(AddPlayerDVDFaceAction::uuid),
			Codec.INT.fieldOf("length_in_ticks").forGetter(AddPlayerDVDFaceAction::lengthInTicks)
	).apply(i, AddPlayerDVDFaceAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (contextMap, player) -> {
			PacketDistributor.sendToPlayer(player, new ClientboundPlayerFaceDVDPackets.Add(uuid, lengthInTicks));
			return true;
		});
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
