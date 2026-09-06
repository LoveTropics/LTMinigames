package com.lovetropics.minigames.common.util;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Stream;

public class Codecs {
	public static final Codec<HolderSet<Item>> ITEMS = RegistryCodecs.homogeneousList(Registries.ITEM);

	private static <T> MapLike<T> emptyMapLike() {
		return new MapLike<>() {
			@Override
			public @Nullable T get(T key) {
				return null;
			}

			@Override
			public @Nullable T get(String key) {
				return null;
			}

			@Override
			public Stream<Pair<T, T>> entries() {
				return Stream.empty();
			}
		};
	}

	public static <A, E> Codec<E> dispatchWithInlineKey(String typeKey, Codec<A> keyCodec, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
		Codec<E> delegate = dispatchMapWithTrace(typeKey, keyCodec, type, codec).codec();
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> ops, T input) {
				DataResult<A> inlineKey = keyCodec.parse(ops, input);
				if (inlineKey.result().isPresent()) {
					return inlineKey.flatMap(key -> {
						return codec.apply(key).decode(ops, emptyMapLike())
								.mapError(err -> "In type " + input + ": " + err)
								.map(b -> Pair.of(b, input));
					});
				}
				return delegate.decode(ops, input);
			}

			@Override
			public <T> DataResult<T> encode(E input, DynamicOps<T> ops, T prefix) {
				return delegate.encode(input, ops, prefix);
			}
		};
	}

	public static <A, E> MapCodec<E> dispatchMapWithTrace(String typeKey, Codec<A> keyCodec, Function<? super E, ? extends A> type, Function<? super A, ? extends MapCodec<? extends E>> codec) {
		KeyDispatchCodec<A, E> delegate = new KeyDispatchCodec<>(keyCodec.fieldOf(typeKey), type.andThen(DataResult::success), codec.andThen(DataResult::success));
		return new MapCodec<>() {
			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return delegate.keys(ops);
			}

			@Override
			public <T> DataResult<E> decode(DynamicOps<T> ops, MapLike<T> input) {
				return delegate.decode(ops, input).mapError(err -> "In type: \"" + input.get(typeKey) + "\": " + err);
			}

			@Override
			public <T> RecordBuilder<T> encode(E input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				return delegate.encode(input, ops, prefix);
			}
		};
	}

	// For debugging
	@SuppressWarnings("unused")
	public static <A> Codec<A> hook(final Codec<A> codec) {
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				return codec.decode(ops, input);
			}

			@Override
			public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
				return codec.encode(input, ops, prefix);
			}
		};
	}

	// Yep.
	public static <A> MapCodec<A> no() {
		return new MapCodec<>() {
			@Override
			public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				return prefix.withErrorsFrom(DataResult.error(() -> "No"));
			}

			@Override
			public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
				return DataResult.error(() -> "No");
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return Stream.empty();
			}
		};
	}

	public static final Codec<Holder<LootTable>> LOOT_TABLE = Codec.either(ItemStackTemplate.CODEC, LootTable.CODEC).xmap(
			either -> either.map(
					template -> {
						LootPoolSingletonContainer.Builder<?> item = LootItem.lootTableItem(template.item().value());
						return Holder.direct(LootTable.lootTable()
								.withPool(LootPool.lootPool()
										.setRolls(ConstantValue.exactly(1))
										.add(item.apply(SetItemCountFunction.setCount(ConstantValue.exactly(template.count()))))
								)
								.build());
					},
					Function.identity()
			),
			Either::right
	);
}
