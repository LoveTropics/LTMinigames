package com.lovetropics.minigames.common.core.game.state;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.behavior.instances.donation.DonationPackageData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.stream.Stream;

public final class GamePackageState implements IGameState {
	public static final GameStateKey.Defaulted<GamePackageState> KEY = GameStateKey.create("Game Packages", GamePackageState::new);

	private final Map<String, DonationPackageData> knownPackages = new Object2ObjectOpenHashMap<>();

	public void addPackageType(final DonationPackageData data) {
		if (knownPackages.putIfAbsent(data.packageType(), data) != null) {
			throw new GameException(Component.literal("Encountered duplicate package with id: " + data.packageType()));
		}
	}

	public Stream<String> stream() {
		return knownPackages.keySet().stream();
	}
}
