package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.world.Difficulty;
import org.lovetropics.dimensions.RuntimeDimensions;
import org.lovetropics.dimensions.SharedDimensionState;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;

public record SetDifficultyNowBehavior(Difficulty difficulty) implements IGameBehavior {
	public static final MapCodec<SetDifficultyNowBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Difficulty.CODEC.fieldOf("difficulty").forGetter(c -> c.difficulty)
	).apply(i, SetDifficultyNowBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			RuntimeDimensions runtimeDimensions = RuntimeDimensions.get(game.server());
			game.allLevels().forEach(serverLevel -> {
				SharedDimensionState sharedState = runtimeDimensions.getSharedStateFor(serverLevel);
				if (sharedState != null) {
					sharedState.setDifficulty(difficulty);
					game.allPlayers().inLevel(serverLevel).sendPacket(new ClientboundChangeDifficultyPacket(difficulty, serverLevel.getLevelData().isDifficultyLocked()));
				}
			});
			return true;
		});
	}
}
