package org.lovetropics.games.common.util;

import net.minecraft.network.protocol.Packet;

public interface LTGameTestFakePlayer {
	void capturePacket(Packet<?> packet);

	boolean shouldRegenerateNaturally();
}
