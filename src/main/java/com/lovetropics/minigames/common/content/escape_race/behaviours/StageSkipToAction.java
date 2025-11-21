package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.FadeToBlackBehaviour;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;

public record StageSkipToAction(
		String namedStage
) implements IGameBehavior {

	public static final MapCodec<StageSkipToAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("stage").forGetter(StageSkipToAction::namedStage)
	).apply(i, StageSkipToAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		NamedStagesBehaviour.State stageState = game.instanceState().getOrNull(NamedStagesBehaviour.KEY);
		if(stageState == null){
			return;
		}
		events.listen(GameActionEvents.APPLY, ((context, targets) -> {
			stageState.progressToStage(namedStage);
			return true;
		}));
	}

}
