package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.data.LoveTropicsAttachments;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Supplier;

public record SetEntityHighlightColorAction(int color) implements IGameBehavior {
	public static final MapCodec<SetEntityHighlightColorAction> CODEC = ExtraCodecs.RGB_COLOR_CODEC
			.fieldOf("color").xmap(SetEntityHighlightColorAction::new, SetEntityHighlightColorAction::color);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToEntities(game, (context, target) -> {
			if (color == 0) {
				target.removeData(LoveTropicsAttachments.HIGHLIGHT_COLOR);
			} else {
				target.setData(LoveTropicsAttachments.HIGHLIGHT_COLOR, color);
			}
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SET_ENTITY_HIGHLIGHT_COLOR;
	}
}
