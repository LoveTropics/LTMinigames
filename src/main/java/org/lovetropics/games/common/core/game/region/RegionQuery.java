package org.lovetropics.games.common.core.game.region;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class RegionQuery {
	public static final Codec<RegionQuery> CODEC = ExtraCodecs.compactListCodec(Codec.STRING, ExtraCodecs.nonEmptyList(Codec.STRING.listOf()))
			.xmap(RegionQuery::new, q -> List.copyOf(q.keys));

	public static final RegionQuery EMPTY = new RegionQuery(Set.of());

	private final Set<String> keys;
	private @Nullable LookupCache lookupCache;

	public RegionQuery(Collection<String> keys) {
		this.keys = Set.copyOf(keys);
	}

	public static RegionQuery of(String... keys) {
		return new RegionQuery(Set.of(keys));
	}

	private LookupCache lookupCache(GameRegionHolder game) {
		LookupCache existingCache = lookupCache;
		if (existingCache != null && existingCache.isValid(game)) {
			return existingCache;
		}
		LookupCache newCache = new LookupCache(game);
		lookupCache = newCache;
		return newCache;
	}

	public Region one(GameRegionHolder game) throws GameException {
		Region region = nullableOne(game);
		if (region == null) {
			throw new GameException(Component.literal("No regions for query: " + this));
		}
		return region;
	}

	public Region oneIn(GameRegionHolder game, Level level) {
		Region region = nullableOneIn(game, level);
		if (region == null) {
			throw new GameException(Component.literal("No regions for query: " + this + " in " + level));
		}
		return region;
	}

	public @Nullable Region nullableOne(GameRegionHolder game) {
		List<Region> regions = all(game);
		return regions.isEmpty() ? null : regions.getFirst();
	}

	public @Nullable Region nullableOneIn(GameRegionHolder game, Level level) {
		List<Region> regions = allIn(game, level);
		return regions.isEmpty() ? null : regions.getFirst();
	}

	public List<Region> all(GameRegionHolder game) {
		return lookupCache(game).getAll();
	}

	public List<Region> allIn(GameRegionHolder game, Level level) {
		return lookupCache(game).getAll(level.dimension());
	}

	public @Nullable LevelBlockPos pickRandomPos(GameRegionHolder game, RandomSource random) {
		Map<ResourceKey<Level>, LongList> blocksByDimension = lookupCache(game).getBlocksByDimension();
		int totalBlockCount = 0;
		for (LongList blocks : blocksByDimension.values()) {
			totalBlockCount += blocks.size();
		}
		if (totalBlockCount == 0) {
			return null;
		}
		int index = random.nextInt(totalBlockCount);
		for (Map.Entry<ResourceKey<Level>, LongList> entry : blocksByDimension.entrySet()) {
			LongList blocks = entry.getValue();
			if (index < blocks.size()) {
				ServerLevel level = Objects.requireNonNull(game.server().getLevel(entry.getKey()));
				return new LevelBlockPos(level, BlockPos.of(blocks.getLong(index)));
			}
			index -= blocks.size();
		}
		return null;
	}

	public void forEachBlock(GameRegionHolder game, BiConsumer<ServerLevel, BlockPos> consumer) {
		LookupCache lookupCache = lookupCache(game);
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		for (Map.Entry<ResourceKey<Level>, LongList> entry : lookupCache.getBlocksByDimension().entrySet()) {
			ServerLevel level = game.server().getLevel(entry.getKey());
			if (level == null) {
				continue;
			}
			entry.getValue().forEach(blockKey ->
					consumer.accept(level, mutablePos.set(blockKey))
			);
		}
	}

	public boolean contains(GameRegionHolder game, ServerLevel level, Vec3 pos) {
		for (Region region : allIn(game, level)) {
			if (region.contains(level, pos)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(GameRegionHolder game, ServerLevel level, BlockPos blockPos) {
		for (Region region : allIn(game, level)) {
			if (region.contains(level, blockPos)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(GameRegionHolder game, Entity entity) {
		for (Region region : allIn(game, entity.level())) {
			if (region.contains(entity)) {
				return true;
			}
		}
		return false;
	}

	public boolean intersects(GameRegionHolder game, Level level, AABB box) {
		for (Region region : allIn(game, level)) {
			if (region.intersects(level, box)) {
				return true;
			}
		}
		return false;
	}

	public Collection<Entity> getEntities(GameRegionHolder game, Predicate<? super Entity> predicate) {
		Set<Entity> entities = new ReferenceOpenHashSet<>();
		for (Region region : all(game)) {
			entities.addAll(region.level().getEntities((Entity) null, region.aabb(), predicate));
		}
		return entities;
	}

	public <E extends Entity> Collection<E> getEntitiesOfClass(GameRegionHolder game, Class<E> entityClass) {
		Set<E> entities = new ReferenceOpenHashSet<>();
		for (Region region : all(game)) {
			entities.addAll(region.level().getEntitiesOfClass(entityClass, region.aabb()));
		}
		return entities;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof RegionQuery query && keys.equals(query.keys);
	}

	@Override
	public int hashCode() {
		return keys.hashCode();
	}

	@Override
	public String toString() {
		return keys.toString();
	}

	private class LookupCache {
		private final WeakReference<GameRegionHolder> game;

		private @Nullable List<Region> regions;
		private final Map<ResourceKey<Level>, List<Region>> regionsByDimension = new Reference2ObjectOpenHashMap<>();
		// Storing every block in each dimension is totally wasteful - we should consider improving it in the future
		private @Nullable Map<ResourceKey<Level>, LongList> blocksByDimension;

		private LookupCache(GameRegionHolder game) {
			this.game = new WeakReference<>(game);
		}

		public List<Region> getAll() {
			if (regions == null) {
				regions = new ArrayList<>();
				GameRegionHolder game = Objects.requireNonNull(this.game.get(), "Reference no longer existed");
				for (String key : keys) {
					regions.addAll(game.gameRegions().get(key));
				}
			}
			return regions;
		}

		public List<Region> getAll(ResourceKey<Level> dimension) {
			List<Region> regions = regionsByDimension.get(dimension);
			if (regions == null) {
				regions = getAll().stream()
						.filter(region -> region.level().dimension() == dimension)
						.toList();
				regionsByDimension.put(dimension, regions);
			}
			return regions;
		}

		public Map<ResourceKey<Level>, LongList> getBlocksByDimension() {
			if (blocksByDimension == null) {
				Reference2ObjectMap<ResourceKey<Level>, LongSet> blockSetsByDimension = new Reference2ObjectOpenHashMap<>();
				for (Region region : getAll()) {
					LongSet blockSet = blockSetsByDimension.computeIfAbsent(region.level().dimension(), _ -> new LongOpenHashSet());
					for (BlockPos pos : region) {
						blockSet.add(pos.asLong());
					}
				}
				blocksByDimension = new Reference2ObjectOpenHashMap<>();
				for (Map.Entry<ResourceKey<Level>, LongSet> entry : blockSetsByDimension.entrySet()) {
					blocksByDimension.put(entry.getKey(), new LongArrayList(entry.getValue()));
				}
			}
			return blocksByDimension;
		}

		public boolean isValid(GameRegionHolder game) {
			return this.game.refersTo(game);
		}
	}
}
