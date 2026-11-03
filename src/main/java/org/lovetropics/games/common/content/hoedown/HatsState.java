package org.lovetropics.games.common.content.hoedown;

import com.lovetropics.lib.item.StackedHat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HatsState implements IGameState {
	private static final GameStateKey.Defaulted<HatsState> KEY = GameStateKey.create("Hats", HatsState::new);

	private final Map<UUID, List<StackedHat>> hatsForTransfer = new HashMap<>();

	public static HatsState get(IGamePhase game) {
		return game.instanceState().get(KEY);
	}

	public void addHats(ServerPlayer player, List<StackedHat> hats) {
		List<StackedHat> playerHats = new ArrayList<>(StackedHat.getEntityHats(player));
		playerHats.addAll(0, hats);
		StackedHat.setEntityHats(player, playerHats);

		PlayerSet playerSet = PlayerSet.of(player);
		playerSet.playSound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.0f);
		playerSet.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
		playerSet.sendMessage(HoedownTexts.ADDED_HAT, true);
	}

	public @Nullable StackedHat removeTopHat(ServerPlayer player) {
		List<StackedHat> playerHats = new ArrayList<>(StackedHat.getEntityHats(player));
		if (playerHats.isEmpty()) {
			return null;
		}
		StackedHat hat = playerHats.removeLast();
		StackedHat.setEntityHats(player, playerHats);
		return hat;
	}

	public void applyHatsFromTransfer(ServerPlayer player) {
		List<StackedHat> hats = hatsForTransfer.remove(player.getUUID());
		if (hats != null) {
			StackedHat.setEntityHats(player, hats);
		}
	}

	public void storeHatsForTransfer(ServerPlayer player) {
		hatsForTransfer.put(player.getUUID(), List.copyOf(StackedHat.getEntityHats(player)));
	}

	public int getHatCount(ServerPlayer player) {
		return StackedHat.getEntityHats(player).size();
	}
}
