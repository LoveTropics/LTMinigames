package com.lovetropics.minigames.common.content.survive_the_tide.entity;

import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import com.lovetropics.minigames.common.core.entity.MinigameEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LightningArrowEntity extends AbstractArrow {
	public LightningArrowEntity(final EntityType<? extends LightningArrowEntity> type, final Level level) {
		super(type, level);
	}

	public LightningArrowEntity(final Level level, final LivingEntity shooter) {
		super(SurviveTheTide.LIGHTNING_ARROW_ENTITY.get(), shooter, level);
	}

	@Override
	protected void onHit(final HitResult result) {
		super.onHit(result);
		final LightningBolt lightning = MinigameEntities.QUIET_LIGHTNING_BOLT.get().create(level());
		final BlockPos hitPos = BlockPos.containing(result.getLocation());
		lightning.moveTo(Vec3.atBottomCenterOf(hitPos));
		level().addFreshEntity(lightning);
	}

	@Override
	protected ItemStack getPickupItem() {
		return new ItemStack(SurviveTheTide.LIGHTNING_ARROW.get());
	}
}
