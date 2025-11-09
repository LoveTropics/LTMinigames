package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.function.Supplier;

public record GiveEffectAction(List<MobEffectInstance> effects) implements IGameBehavior {
	public static final MapCodec<GiveEffectAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.EFFECT_INSTANCE.listOf().fieldOf("effects").forGetter(c -> c.effects)
	).apply(i, GiveEffectAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY_TO_ENTITY, (context, entity) -> {
			if (!(entity instanceof LivingEntity livingEntity)) {
				return false;
			}
			for (MobEffectInstance effect : effects) {
				livingEntity.addEffect(new MobEffectInstance(effect));
			}
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.GIVE_EFFECT;
	}
}
