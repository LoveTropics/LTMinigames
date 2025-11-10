package com.lovetropics.minigames.common.core.game.behavior.event;

import com.google.gson.JsonObject;
import com.lovetropics.lib.techstack.Crud;
import com.lovetropics.minigames.common.core.integration.game_actions.Donation;
import com.lovetropics.minigames.common.core.integration.game_actions.GamePackage;
import net.minecraft.util.TriState;

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

	public static final GameEventType<ReceivePollEvent> RECEIVE_POLL_EVENT = GameEventType.create(ReceivePollEvent.class, listeners -> (object, crud) -> {
		for (ReceivePollEvent listener : listeners) {
			listener.onReceivePollEvent(object, crud);
		}
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

	public interface ReceivePollEvent {
		void onReceivePollEvent(JsonObject object, Crud crud);
	}

	public interface ReceiveDonation {
		void onReceiveDonation(Donation donation);
	}
}
