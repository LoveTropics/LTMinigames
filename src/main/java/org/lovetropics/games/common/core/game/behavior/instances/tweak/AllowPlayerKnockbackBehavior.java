package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;

public record AllowPlayerKnockbackBehavior() implements IGameBehavior {
	public static final MapCodec<AllowPlayerKnockbackBehavior> CODEC = MapCodec.unit(AllowPlayerKnockbackBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		// Override CancelPlayerDamageBehavior
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> target instanceof Player ? TriState.TRUE : TriState.DEFAULT);
		events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> TriState.TRUE);
		events.listen(GamePlayerEvents.DAMAGE_AMOUNT, (player, damageSource, amount, originalAmount) -> 0.0f);
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
