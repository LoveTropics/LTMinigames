package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.GameType;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;

public record SetGameModeAction(
		GameType gameMode
) implements IGameBehavior {
	public static final MapCodec<SetGameModeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameType.CODEC.fieldOf("game_mode").forGetter(SetGameModeAction::gameMode)
	).apply(i, SetGameModeAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, target) -> {
			target.setGameMode(gameMode);
			return true;
		});
	}
}
