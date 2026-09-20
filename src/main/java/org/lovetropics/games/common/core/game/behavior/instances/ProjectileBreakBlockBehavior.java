package org.lovetropics.games.common.core.game.behavior.instances;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Supplier;

public record ProjectileBreakBlockBehavior(
		EntityPredicate projectilePredicate,
		BlockPredicate blockPredicate
) implements IGameBehavior {
	public static final MapCodec<ProjectileBreakBlockBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.fieldOf("projectile").forGetter(ProjectileBreakBlockBehavior::projectilePredicate),
			BlockPredicate.CODEC.fieldOf("block").forGetter(ProjectileBreakBlockBehavior::blockPredicate)
	).apply(i, ProjectileBreakBlockBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.PROJECTILE_IMPACT, (level, projectile, hitResult) -> {
			if (hitResult.getType() != HitResult.Type.BLOCK) {
				return;
			}
			BlockHitResult blockHitResult = (BlockHitResult) hitResult;
			BlockPos blockPos = blockHitResult.getBlockPos();
			if (blockPredicate.matches(level, blockPos) && projectilePredicate.matches(level, null, projectile)) {
				level.removeBlock(blockPos, false);
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.PROJECTILE_BREAK_BLOCK;
	}
}
