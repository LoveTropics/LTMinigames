package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.GameType;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;

import java.util.Optional;

/// @param from only changes the game mode of players who are in this one like to leave spectators alone
public record SetGameModeAction(
		GameType gameMode,
		Optional<GameType> from
) implements IGameBehavior {
	public static final MapCodec<SetGameModeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameType.CODEC.fieldOf("game_mode").forGetter(SetGameModeAction::gameMode),
			GameType.CODEC.optionalFieldOf("from").forGetter(SetGameModeAction::from)
	).apply(i, SetGameModeAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, target) -> {
			if (from.isPresent() && target.gameMode.getGameModeForPlayer() != from.get()) {
				return false;
			}
			target.setGameMode(gameMode);
			return true;
		});
	}
}
