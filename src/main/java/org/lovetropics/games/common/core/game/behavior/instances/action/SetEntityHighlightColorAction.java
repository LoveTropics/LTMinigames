package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.ExtraCodecs;
import org.lovetropics.games.common.core.data.LoveTropicsAttachments;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;

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
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
