package org.lovetropics.games.common.core.game.behavior.event;

import net.minecraft.util.TriState;
import org.lovetropics.games.common.core.integration.game_actions.Donation;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;

public final class GamePackageEvents {
	public static final GameEventType<ReceivePackage> RECEIVE_PACKAGE = GameEventType.create(ReceivePackage.class, listeners -> (gamePackage) -> {
		for (ReceivePackage listener : listeners) {
			TriState result = listener.onReceivePackage(gamePackage);
			if (!result.isDefault()) {
				return result;
			}
		}
		return TriState.FALSE;
	});

	public static final GameEventType<ReceiveDonation> RECEIVE_DONATION = GameEventType.create(ReceiveDonation.class, listeners -> donation -> {
		for (ReceiveDonation listener : listeners) {
			listener.onReceiveDonation(donation);
		}
	});

	private GamePackageEvents() {
	}

	public interface ReceivePackage {
		TriState onReceivePackage(GamePackage gamePackage);
	}

	public interface ReceiveDonation {
		void onReceiveDonation(Donation donation);
	}
}
