package com.lovetropics.minigames.common.content.biodiversity_blitz.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class BrambleBlock extends BushBlock {
	public BrambleBlock(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public MapCodec<BushBlock> codec() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
		if (!(entity instanceof LivingEntity) || entity.getType() == EntityType.PLAYER) {
			return;
		}
		entity.makeStuckInBlock(state, new Vec3(0.8F, 0.75D, 0.8F));
		if (level instanceof ServerLevel serverLevel) {
			Vec3 movement = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.oldPosition().subtract(entity.position());
			if (movement.horizontalDistanceSqr() > 0.0 && (Math.abs(movement.x()) >= 0.003f || Math.abs(movement.z()) >= 0.003f)) {
				entity.hurtServer(serverLevel, serverLevel.damageSources().sweetBerryBush(), 1.0f);
			}
		}
	}

	@Override
	public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
		return PathType.DANGER_OTHER;
	}
}
