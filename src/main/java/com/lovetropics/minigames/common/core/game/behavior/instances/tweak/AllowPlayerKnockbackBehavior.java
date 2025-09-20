package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

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
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.ALLOW_PLAYER_KNOCKBACK;
	}
}
