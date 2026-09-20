package org.lovetropics.games.common.core.game.state;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.behavior.instances.donation.DonationPackageData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public final class GamePackageState implements IGameState {
	public static final GameStateKey.Defaulted<GamePackageState> KEY = GameStateKey.create("Game Packages", GamePackageState::new);

	private final Map<String, DonationPackageData> knownPackages = new Object2ObjectOpenHashMap<>();

	public void addPackageType(DonationPackageData data) {
		if (knownPackages.putIfAbsent(data.id(), data) != null) {
			throw new GameException(Component.literal("Encountered duplicate package with id: " + data.id()));
		}
	}

	public Stream<String> keys() {
		return knownPackages.keySet().stream();
	}

	public Collection<DonationPackageData> packages() {
		return knownPackages.values();
	}
}
