package org.lovetropics.games.common.content.hoedown;

import com.lovetropics.lib.item.StackedHat;
import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.PickUpResult;

import java.util.List;

public record HatPickupBehavior(
) implements IGameBehavior {
	public static final MapCodec<HatPickupBehavior> CODEC = MapCodec.unit(HatPickupBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		HatsState hatsState = HatsState.get(game);
		events.listen(GamePlayerEvents.PICK_UP_ITEM, (player, item) -> {
			List<StackedHat> hats = StackedHat.get(item.getItem());
			if (!hats.isEmpty()) {
				hatsState.addHats(player, hats);
				return PickUpResult.DISCARD;
			}
			return PickUpResult.PASS;
		});
	}
}
