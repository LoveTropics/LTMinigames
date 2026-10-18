package org.lovetropics.games.common.core.network.shop;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.mcsr.shop.Shop;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;

import java.util.Optional;
import java.util.UUID;

/// Buys an offer from the shop of the game the player is in
///
/// @param target the player to use the offer on, if the buyer picked one
public record ServerboundShopPurchasePacket(int offer, Optional<UUID> target) implements CustomPacketPayload {
	public static final Type<ServerboundShopPurchasePacket> TYPE = new Type<>(LoveTropics.id("shop_purchase"));
	public static final StreamCodec<ByteBuf, ServerboundShopPurchasePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ServerboundShopPurchasePacket::offer,
			ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), ServerboundShopPurchasePacket::target,
			ServerboundShopPurchasePacket::new
	);

	public static void handle(ServerboundShopPurchasePacket message, IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		IGamePhase game = IGameLookup.get().getGamePhaseFor(player);
		// The shop is shared by all phases of the game, as players may be in a sub-phase
		Shop shop = game != null ? game.instanceState().getOrNull(Shop.KEY) : null;
		if (shop != null) {
			shop.purchase(player, message.offer, message.target);
		}
	}

	@Override
	public Type<ServerboundShopPurchasePacket> type() {
		return TYPE;
	}
}
