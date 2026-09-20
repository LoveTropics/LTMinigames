package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;

import java.util.function.Supplier;

public record DisableHungerBehavior() implements IGameBehavior {
	public static final MapCodec<DisableHungerBehavior> CODEC = MapCodec.unit(DisableHungerBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.tickCount % SharedConstants.TICKS_PER_SECOND == 0) {
				HungerResetter.reset(player);
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.DISABLE_HUNGER;
	}
}
