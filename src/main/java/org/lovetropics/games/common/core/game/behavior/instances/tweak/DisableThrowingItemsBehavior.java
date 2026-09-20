package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.TriState;

public record DisableThrowingItemsBehavior() implements IGameBehavior {
	public static final MapCodec<DisableThrowingItemsBehavior> CODEC = MapCodec.unit(DisableThrowingItemsBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.THROW_ITEM, (player, item) -> TriState.FALSE);
	}
}
