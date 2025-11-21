package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StageProgressAction(
		boolean skipEndActions
) implements IGameBehavior {
	public static final MapCodec<StageProgressAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("skip_end_actions", false).forGetter(StageProgressAction::skipEndActions)
	).apply(i, StageProgressAction::new));
	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		NamedStagesBehaviour.State stageState = game.instanceState().getOrNull(NamedStagesBehaviour.KEY);
		if(stageState == null){
			return;
		}
		events.listen(GameActionEvents.APPLY, ((context, targets) -> {
			stageState.progressToNext(skipEndActions);
			return true;
		}));
	}


}
