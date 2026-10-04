package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.Difficulty;
import org.lovetropics.dimensions.RuntimeDimensions;
import org.lovetropics.dimensions.SharedDimensionState;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;

public record SetDifficultyBehavior(Difficulty difficulty) implements IGameBehavior {
	public static final MapCodec<SetDifficultyBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Difficulty.CODEC.fieldOf("difficulty").forGetter(c -> c.difficulty)
	).apply(i, SetDifficultyBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, _ -> {
			SharedDimensionState sharedState = RuntimeDimensions.get(game.server()).getSharedStateFor(game.level());
			if (sharedState != null) {
				sharedState.setDifficulty(difficulty);
			}
		});
	}
}
