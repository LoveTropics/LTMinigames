package com.lovetropics.minigames.common.util;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;
import java.util.function.Predicate;

public interface BlockStatePredicate extends Predicate<BlockState> {
	Codec<BlockStatePredicate> CODEC = new Codec<>() {
		@Override
		public <T> DataResult<Pair<BlockStatePredicate, T>> decode(DynamicOps<T> ops, T input) {
			DataResult<Pair<AnyOf, T>> any = AnyOf.CODEC.decode(ops, input);
			if (any.result().isPresent()) {
				return any.map(p -> p.mapFirst(Function.identity()));
			}

			DataResult<Pair<MatchesBlocks, T>> tag = MatchesBlocks.CODEC.decode(ops, input);
			if (tag.result().isPresent()) {
				return tag.map(p -> p.mapFirst(Function.identity()));
			}

			return MatchesBlocks.CODEC.decode(ops, input).map(p -> p.mapFirst(Function.identity()));
		}

		@Override
		public <T> DataResult<T> encode(BlockStatePredicate predicate, DynamicOps<T> ops, T prefix) {
			return encodeUnchecked(predicate.getCodec(), predicate, ops, prefix);
		}
	};

	BlockStatePredicate ANY = new Any();

	@Override
	boolean test(BlockState state);

	Codec<? extends BlockStatePredicate> getCodec();

	@SuppressWarnings("unchecked")
	static <A, T> DataResult<T> encodeUnchecked(Codec<A> codec, Object input, DynamicOps<T> ops, T prefix) {
		return codec.encode((A) input, ops, prefix);
	}

	record MatchesBlocks(HolderSet<Block> blocks) implements BlockStatePredicate {
		public static final Codec<MatchesBlocks> CODEC = RegistryCodecs.homogeneousList(Registries.BLOCK)
				.xmap(MatchesBlocks::new, matchesBlocks -> matchesBlocks.blocks);

		@Override
		public boolean test(BlockState state) {
			return state.is(blocks);
		}

		@Override
		public Codec<? extends BlockStatePredicate> getCodec() {
			return CODEC;
		}
	}

	final class AnyOf implements BlockStatePredicate {
		public static final Codec<AnyOf> CODEC = MoreCodecs.listToArray(BlockStatePredicate.CODEC.listOf(), BlockStatePredicate[]::new)
				.xmap(AnyOf::new, c -> c.predicates);

		private final BlockStatePredicate[] predicates;

		public AnyOf(BlockStatePredicate[] predicates) {
			this.predicates = predicates;
		}

		@Override
		public boolean test(BlockState state) {
			for (BlockStatePredicate predicate : predicates) {
				if (predicate.test(state)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Codec<? extends BlockStatePredicate> getCodec() {
			return CODEC;
		}
	}

	final class Any implements BlockStatePredicate {
		public static final Codec<Any> CODEC = MapCodec.unitCodec(Any::new);

		@Override
		public boolean test(BlockState state) {
			return true;
		}

		@Override
		public Codec<? extends BlockStatePredicate> getCodec() {
			return CODEC;
		}
	}
}
