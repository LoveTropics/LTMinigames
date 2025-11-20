package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

public class CrabGolfCrabBehavior implements PersistentGameBehavior {
	public static final MapCodec<CrabGolfCrabBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.withAlternative(CompoundTag.CODEC, TagParser.FLATTENED_CODEC).fieldOf("nbt").forGetter(b -> b.nbt)
	).apply(instance, CrabGolfCrabBehavior::new));

	private final CompoundTag nbt;

	public CrabGolfCrabBehavior(CompoundTag nbt) {
		this.nbt = nbt;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(CrabGolfEvents.SUMMON_CRAB, (level, pos) -> {
			CompoundTag nbt = this.nbt.copy();
			nbt.putString("id", "tropicraft:fiddler_crab");

			LivingEntity entity = (LivingEntity) EntityType.loadEntityRecursive(nbt, level, EntitySpawnReason.COMMAND, (e) -> {
				e.snapTo(pos.x, pos.y, pos.z, e.getYRot(), e.getXRot());
				return e;
			});

			level.tryAddFreshEntityWithPassengers(entity);

			return entity;
		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CRAB_GOLF_CRAB;
	}
}
