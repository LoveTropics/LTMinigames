package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import javax.annotation.Nullable;
import java.util.Optional;

public record SetGameTypesBehavior(Optional<GameType> participantGameType, Optional<GameType> spectatorGameType, GameType allGameType) implements IGameBehavior {
	public static final MapCodec<SetGameTypesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameType.CODEC.optionalFieldOf("participant").forGetter(c -> c.participantGameType),
			GameType.CODEC.optionalFieldOf("spectator").forGetter(c -> c.spectatorGameType),
			GameType.CODEC.optionalFieldOf("all", GameType.ADVENTURE).forGetter(c -> c.allGameType)
	).apply(i, SetGameTypesBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> applyRoleTo(player, role));
	}

	private void applyRoleTo(ServerPlayer player, @Nullable PlayerRole role) {
		GameType gameType;
		if (role == PlayerRole.PARTICIPANT) {
			gameType = participantGameType.orElse(allGameType);
		} else if (role == PlayerRole.SPECTATOR) {
			gameType = spectatorGameType.orElse(allGameType);
		} else {
			gameType = allGameType;
		}
		player.setGameMode(gameType);
	}
}
