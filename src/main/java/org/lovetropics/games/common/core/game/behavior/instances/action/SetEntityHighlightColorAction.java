package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.data.LoveTropicsAttachments;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.function.Supplier;

public record SetEntityHighlightColorAction(int color) implements IGameBehavior {
	public static final MapCodec<SetEntityHighlightColorAction> CODEC = ExtraCodecs.RGB_COLOR_CODEC
			.fieldOf("color").xmap(SetEntityHighlightColorAction::new, SetEntityHighlightColorAction::color);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToEntities(game, (context, level, target) -> {
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
