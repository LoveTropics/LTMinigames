package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;

public record ClearDisguiseAction(Disguise disguise) implements IGameBehavior {
	public static final MapCodec<ClearDisguiseAction> CODEC = Disguise.MAP_CODEC.xmap(ClearDisguiseAction::new, ClearDisguiseAction::disguise);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToEntities(game, (context, entity) -> {
			// Nothing specified to clear - just clear everything
			if (disguise.isEmpty()) {
				EntityDisguiseHolder.set(entity, Disguise.NONE);
			} else {
				EntityDisguiseHolder.update(entity, d -> d.clear(disguise));
			}
			return true;
		});
	}
}
