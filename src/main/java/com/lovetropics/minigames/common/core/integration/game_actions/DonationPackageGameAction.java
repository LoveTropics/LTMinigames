package com.lovetropics.minigames.common.core.integration.game_actions;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TriState;
import org.slf4j.Logger;

/// Care package
public record DonationPackageGameAction(GamePackage gamePackage) implements GameAction {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<DonationPackageGameAction> CODEC = GamePackage.MAP_CODEC
			.xmap(DonationPackageGameAction::new, DonationPackageGameAction::gamePackage);

	@Override
	public boolean resolve(IGamePhase game, MinecraftServer server) {
		TriState result = game.invoker(GamePackageEvents.RECEIVE_PACKAGE).onReceivePackage(gamePackage);
		switch (result) {
			case TRUE ->
					LOGGER.debug("Incoming donation package was successfully processed by behavior: {}", gamePackage);
			case DEFAULT -> LOGGER.debug("Incoming donation package was not handled by behavior: {}", gamePackage);
			case FALSE -> LOGGER.debug("Incoming donation package was rejected by behavior: {}", gamePackage);
		}
		return result.isTrue();
	}
}
