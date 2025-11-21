package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public record ClearEffectsAction(Optional<List<Holder<MobEffect>>> effects) implements IGameBehavior {
	public static final MapCodec<ClearEffectsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MobEffect.CODEC.listOf().optionalFieldOf("effects").forGetter(ClearEffectsAction::effects)
	).apply(i, ClearEffectsAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToEntities(game, (context, entity) -> {
			if (!(entity instanceof LivingEntity livingEntity)) {
				return false;
			}
			if (livingEntity.getActiveEffects().isEmpty()) {
				return false;
			}
			if (effects.isEmpty()) {
				livingEntity.removeAllEffects();
			} else {
				for (Holder<MobEffect> effect : effects.get()) {
					livingEntity.removeEffect(effect);
				}
			}
			return true;
		});
	}
}
