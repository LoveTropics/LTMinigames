package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;

import java.util.List;

public record GiveEffectAction(List<MobEffectInstance> effects) implements IGameBehavior {
	public static final MapCodec<GiveEffectAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.EFFECT_INSTANCE.listOf().fieldOf("effects").forGetter(c -> c.effects)
	).apply(i, GiveEffectAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToEntities(game, (context, level, entity) -> {
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
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
