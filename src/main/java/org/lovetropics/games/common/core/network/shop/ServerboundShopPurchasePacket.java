package org.lovetropics.games.common.core.network.shop;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.mcsr.shop.Shop;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;

/// Buys an offer from the shop of the game the player is in
public record ServerboundShopPurchasePacket(int offer) implements CustomPacketPayload {
	public static final Type<ServerboundShopPurchasePacket> TYPE = new Type<>(LoveTropics.id("shop_purchase"));
	public static final StreamCodec<ByteBuf, ServerboundShopPurchasePacket> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ServerboundShopPurchasePacket::offer,
			ServerboundShopPurchasePacket::new
	);

	public static void handle(ServerboundShopPurchasePacket message, IPayloadContext context) {
		ServerPlayer player = (ServerPlayer) context.player();
		IGamePhase game = IGameLookup.get().getGamePhaseFor(player);
		// The shop is shared by all phases of the game, as players may be in a sub-phase
		Shop shop = game != null ? game.instanceState().getOrNull(Shop.KEY) : null;
		if (shop != null) {
			shop.purchase(player, message.offer);
		}
	}

	@Override
	public Type<ServerboundShopPurchasePacket> type() {
		return TYPE;
	}
}
