package org.lovetropics.games.common.core.game.region;

import com.lovetropics.lib.BlockBox;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
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

	public BlockBox one(GameRegionHolder game) throws GameException {
		BlockBox region = nullableOne(game);
		if (region == null) {
			throw new GameException(Component.literal("No regions for query: " + this));
		}
		return region;
	}

	public @Nullable BlockBox nullableOne(GameRegionHolder game) {
		List<BlockBox> regions = all(game);
		return regions.isEmpty() ? null : regions.getFirst();
	}

	public List<BlockBox> all(GameRegionHolder game) {
		return lookupCache(game).getAll();
	}

	public @Nullable BlockPos pickRandomPos(GameRegionHolder game, RandomSource random) {
		LongList blocks = lookupCache(game).getBlocks();
		if (blocks.isEmpty()) {
			return null;
		}
		return BlockPos.of(blocks.getLong(random.nextInt(blocks.size())));
	}

	public void forEachBlock(GameRegionHolder game, Consumer<BlockPos> consumer) {
		LookupCache lookupCache = lookupCache(game);
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		lookupCache.getBlocks().forEach(blockKey ->
				consumer.accept(mutablePos.set(blockKey))
		);
	}

	public boolean contains(GameRegionHolder game, ServerLevel level, Vec3 pos) {
		if (game.level() != level) {
			return false;
		}
		for (BlockBox region : all(game)) {
			if (region.contains(pos)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(GameRegionHolder game, ServerLevel level, BlockPos blockPos) {
		if (game.level() != level) {
			return false;
		}
		for (BlockBox region : all(game)) {
			if (region.contains(blockPos)) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(GameRegionHolder game, Entity entity) {
		if (game.level() != entity.level()) {
			return false;
		}
		for (BlockBox region : all(game)) {
			if (region.contains(entity.position())) {
				return true;
			}
		}
		return false;
	}

	public boolean intersects(GameRegionHolder game, Level level, AABB box) {
		if (game.level() != level) {
			return false;
		}
		for (BlockBox region : all(game)) {
			if (region.intersects(box)) {
				return true;
			}
		}
		return false;
	}

	public Collection<Entity> getEntities(GameRegionHolder game, Predicate<? super Entity> predicate) {
		Set<Entity> entities = new ReferenceOpenHashSet<>();
		for (BlockBox region : all(game)) {
			entities.addAll(game.level().getEntities((Entity) null, region.asAabb(), predicate));
		}
		return entities;
	}

	public <E extends Entity> Collection<E> getEntitiesOfClass(GameRegionHolder game, Class<E> entityClass) {
		Set<E> entities = new ReferenceOpenHashSet<>();
		for (BlockBox region : all(game)) {
			entities.addAll(game.level().getEntitiesOfClass(entityClass, region.asAabb()));
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

		private @Nullable List<BlockBox> regions;
		// Storing every block is totally wasteful - we should consider improving it in the future
		private @Nullable LongList blocks;

		private LookupCache(GameRegionHolder game) {
			this.game = new WeakReference<>(game);
		}

		public List<BlockBox> getAll() {
			if (regions == null) {
				regions = new ArrayList<>();
				GameRegionHolder game = Objects.requireNonNull(this.game.get(), "Reference no longer existed");
				for (String key : keys) {
					regions.addAll(game.gameRegions().get(key));
				}
			}
			return regions;
		}

		public LongList getBlocks() {
			if (blocks == null) {
				LongSet blockSet = new LongOpenHashSet();
				for (BlockBox region : getAll()) {
					for (BlockPos pos : region) {
						blockSet.add(pos.asLong());
					}
				}
				blocks = new LongArrayList(blockSet);
			}
			return blocks;
		}

		public boolean isValid(GameRegionHolder game) {
			return this.game.refersTo(game);
		}
	}
}
