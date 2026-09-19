package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.List;

public record EffectAddingPlantBehavior(MobEffectInstance effect, double radius) implements IGameBehavior {
	public static final MapCodec<EffectAddingPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.EFFECT_INSTANCE.fieldOf("effect").forGetter(c -> c.effect),
			Codec.DOUBLE.fieldOf("radius").forGetter(c -> c.radius)
	).apply(i, EffectAddingPlantBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		int effectDuration = effect.getDuration();
		int applyTime = Math.max(effectDuration - 5, 1);

		events.listen(BbPlantEvents.TICK, (players, plot, plants) -> {
			long ticks = game.ticks();
			if (ticks % applyTime != 0) {
				return;
			}

			for (Plant plant : plants) {
				AABB applyBounds = plant.coverage().asBounds().inflate(radius);

				List<Mob> entities = plot.level.getEntitiesOfClass(Mob.class, applyBounds, BbMobEntity.PREDICATE);
				for (Mob entity : entities) {
					entity.addEffect(new MobEffectInstance(effect));
				}
			}
		});
	}
}
