package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.TriState;

public record DisableThrowingItemsBehavior() implements IGameBehavior {
	public static final MapCodec<DisableThrowingItemsBehavior> CODEC = MapCodec.unit(DisableThrowingItemsBehavior::new);

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		events.listen(GamePlayerEvents.THROW_ITEM, (player, item) -> TriState.FALSE);
	}
}
