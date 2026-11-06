package org.lovetropics.games.common.core.game.behavior.instances.stages;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;

public record StageSkipToAction(
		String namedStage,
		boolean ignoreEndActions
) implements IGameBehavior {

	public static final MapCodec<StageSkipToAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("stage").forGetter(StageSkipToAction::namedStage),
			Codec.BOOL.optionalFieldOf("ignore_end_actions", false).forGetter(StageSkipToAction::ignoreEndActions)
	).apply(i, StageSkipToAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		NamedStagesBehaviour.State stageState = game.instanceState().getOrNull(NamedStagesBehaviour.KEY);
		if(stageState == null){
			return;
		}
		events.listen(GameActionEvents.APPLY, ((context, targets) -> {
			stageState.progressToStage(namedStage, ignoreEndActions);
			return true;
		}));
	}

}
