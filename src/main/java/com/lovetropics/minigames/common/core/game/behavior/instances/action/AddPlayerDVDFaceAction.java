package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.network.ClientboundPlayerFaceDVDPackets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

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
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ADD_DVD_FACE;
	}
}
