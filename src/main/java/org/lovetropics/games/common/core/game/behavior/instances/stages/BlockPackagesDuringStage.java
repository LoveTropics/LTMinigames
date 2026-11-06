package org.lovetropics.games.common.core.game.behavior.instances.stages;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePackageEvents;

import java.util.List;

public record BlockPackagesDuringStage(
		List<String> blockedStages
) implements IGameBehavior {

	public static final MapCodec<BlockPackagesDuringStage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("stages").forGetter(BlockPackagesDuringStage::blockedStages)
	).apply(i, BlockPackagesDuringStage::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		NamedStagesBehaviour.State stageState = game.instanceState().getOrNull(NamedStagesBehaviour.KEY);
		MutableBoolean gameOver = new MutableBoolean();
		events.listen(GameLogicEvents.GAME_OVER, winner -> gameOver.setTrue());

		events.listen(GamePackageEvents.RECEIVE_PACKAGE, gamePackage -> {
			if(stageState != null && stageState.getCurrentStageState() != null &&
					blockedStages.contains(stageState.getCurrentStageState().id()) || gameOver.isTrue()){
				return TriState.FALSE;
			}
			return TriState.DEFAULT;
		});
	}

}
