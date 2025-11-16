package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.tags.DamageTypeTags;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record AllowSingleFallAction() implements IGameBehavior {
	public static final MapCodec<AllowSingleFallAction> CODEC = MapCodec.unit(AllowSingleFallAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		Set<UUID> playersAllowedToFall = new HashSet<>();

		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.onGround()) {
				playersAllowedToFall.remove(player.getUUID());
			}
		});

		events.listen(GamePlayerEvents.DAMAGE_AMOUNT, (player, damageSource, amount, originalAmount) -> {
			if (damageSource.is(DamageTypeTags.IS_FALL) && playersAllowedToFall.remove(player.getUUID())) {
				return 0.0f;
			}
			return amount;
		});

		events.applyToPlayers(game, (context, target) -> {
			playersAllowedToFall.add(target.getUUID());
			return true;
		});
	}
}
