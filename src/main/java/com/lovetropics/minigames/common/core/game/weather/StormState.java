package com.lovetropics.minigames.common.core.game.weather;

import net.minecraft.network.PacketBuffer;

public final class StormState {
	private final int buildupTickRate;
	private final int maxStackable;

	public StormState(int buildupTickRate, int maxStackable) {
		this.buildupTickRate = buildupTickRate;
		this.maxStackable = maxStackable;
	}

	public int getBuildupTickRate() {
		return buildupTickRate;
	}

	public int getMaxStackable() {
		return maxStackable;
	}

	public void encode(PacketBuffer buffer) {
		buffer.writeVarInt(this.buildupTickRate);
		buffer.writeVarInt(this.maxStackable);
	}

	public static StormState decode(PacketBuffer buffer) {
		int buildupTickRate = buffer.readVarInt();
		int maxStackable = buffer.readVarInt();
		return new StormState(buildupTickRate, maxStackable);
	}
}
