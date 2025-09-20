package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.common.core.game.client_state.instance.ResourcePackClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class GameResourcePackHandler implements ClientGameStateHandler<ResourcePackClientState> {
	public static final GameResourcePackHandler INSTANCE = new GameResourcePackHandler();

	private GameResourcePackHandler() {
	}

	@Override
	public void accept(ResourcePackClientState state) {
		String pack = state.packName();
		if (packExists(pack)) {
			updatePacks(enabledPacks -> {
				if (!enabledPacks.contains(pack)) {
					enabledPacks.add(pack);
					return true;
				} else {
					return false;
				}
			});
		}
	}

	@Override
	public void disable(ResourcePackClientState state) {
		String pack = state.packName();
		if (packExists(pack)) {
			updatePacks(enabledPacks -> enabledPacks.remove(pack));
		}
	}

	private void updatePacks(Predicate<List<String>> apply) {
		PackRepository packList = Minecraft.getInstance().getResourcePackRepository();

		List<String> enabledPacks = packList.getSelectedPacks().stream()
				.map(Pack::getId)
				.collect(Collectors.toList());

		if (apply.test(enabledPacks)) {
			packList.setSelected(enabledPacks);

			Minecraft.getInstance().delayTextureReload();
		}
	}

	private boolean packExists(String packName) {
		return Minecraft.getInstance().getResourcePackRepository().isAvailable(packName);
	}
}
