package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IActiveGame;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import net.minecraft.util.ActionResultType;

public final class CancelPlayerDamageBehavior implements IGameBehavior {
	public static final Codec<CancelPlayerDamageBehavior> CODEC = Codec.unit(CancelPlayerDamageBehavior::new);

	@Override
	public void register(IActiveGame registerGame, EventRegistrar events) {
		events.listen(GamePlayerEvents.DAMAGE, (game, player, damageSource, amount) -> ActionResultType.FAIL);
	}
}
