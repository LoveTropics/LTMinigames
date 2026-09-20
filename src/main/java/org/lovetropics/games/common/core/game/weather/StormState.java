package org.lovetropics.games.common.core.game.weather;

import net.minecraft.network.FriendlyByteBuf;

public record StormState(int buildupTickRate, int maxStackable) {
	public void encode(FriendlyByteBuf buffer) {
		buffer.writeVarInt(buildupTickRate);
		buffer.writeVarInt(maxStackable);
	}

	public static StormState decode(FriendlyByteBuf buffer) {
		int buildupTickRate = buffer.readVarInt();
		int maxStackable = buffer.readVarInt();
		return new StormState(buildupTickRate, maxStackable);
	}
}
