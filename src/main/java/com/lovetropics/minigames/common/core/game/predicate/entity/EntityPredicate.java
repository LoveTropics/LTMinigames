package com.lovetropics.minigames.common.core.game.predicate.entity;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;

import java.util.function.Function;

public interface EntityPredicate {
	Codec<EntityPredicate> DIRECT_CODEC = Codec.lazyInitialized(() -> EntityPredicates.REGISTRY.byNameCodec())
			.dispatch(EntityPredicate::codec, Function.identity());
	Codec<EntityPredicate> CODEC_WITH_FALLBACK = Codec.xor(DIRECT_CODEC, BuiltInRegistries.ENTITY_TYPE.byNameCodec())
			.xmap(either -> either.map(Function.identity(), EntityTypeEntityPredicate::new), Either::left);

	boolean test(Entity entity);

	MapCodec<? extends EntityPredicate> codec();
}
