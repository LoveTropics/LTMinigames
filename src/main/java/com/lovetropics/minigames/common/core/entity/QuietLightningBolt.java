package com.lovetropics.minigames.common.core.entity;

import com.google.common.collect.Sets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class QuietLightningBolt extends LightningBolt {
	private final Set<Entity> hitEntities = Sets.newHashSet();

	private int life;
	private int flashes;

	public QuietLightningBolt(EntityType<? extends LightningBolt> type, Level level) {
		super(type, level);
		life = 2;
		flashes = random.nextInt(3) + 1;
	}

	@Override
	public void tick() {
		baseTick();

		if (life == 2) {
			if (level().isClientSide()) {
				level().playLocalSound(getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4.0f, 0.8f + random.nextFloat() * 0.2F, false);
				level().playLocalSound(getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0f, 0.5f + random.nextFloat() * 0.2F, false);
			}
		}

		--life;
		if (life < 0) {
			if (flashes == 0) {
				discard();
			} else if (life < -random.nextInt(10)) {
				--flashes;
				life = 1;
				seed = random.nextLong();
			}
		}

		if (life >= 0) {
			if (level() instanceof ServerLevel serverLevel) {
				List<Entity> entities = level().getEntities(this, new AABB(getX() - 3.0D, getY() - 3.0D, getZ() - 3.0D, getX() + 3.0D, getY() + 6.0D + 3.0D, getZ() + 3.0D), Entity::isAlive);
				for (Entity entity : entities) {
					if (!EventHooks.onEntityStruckByLightning(entity, this)) {
						entity.thunderHit(serverLevel, this);
					}
				}

				hitEntities.addAll(entities);
			} else {
				level().setSkyFlashTime(0);
			}
		}
	}

	@Override
	public Stream<Entity> getHitEntities() {
		return hitEntities.stream().filter(Entity::isAlive);
	}
}
