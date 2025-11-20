package com.lovetropics.minigames.common.core.map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MapRegions {
	private static final Codec<BlockPos> LEGACY_POS_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("x").forGetter(Vec3i::getX),
			Codec.INT.fieldOf("y").forGetter(Vec3i::getY),
			Codec.INT.fieldOf("z").forGetter(Vec3i::getZ)
	).apply(i, BlockPos::new));
	public static final Codec<BlockBox> LEGACY_BOX_CODEC = RecordCodecBuilder.create(i -> i.group(
			LEGACY_POS_CODEC.fieldOf("min").forGetter(BlockBox::min),
			LEGACY_POS_CODEC.fieldOf("max").forGetter(BlockBox::max)
	).apply(i, BlockBox::new));

	private static final Codec<BlockBox> BOX_CODEC = Codec.withAlternative(BlockBox.CODEC, LEGACY_BOX_CODEC);

	public static final Codec<MapRegions> CODEC = Codec.unboundedMap(Codec.STRING, BOX_CODEC.listOf()).xmap(
			map -> {
				MapRegions regions = new MapRegions();
				for (Map.Entry<String, List<BlockBox>> entry : map.entrySet()) {
					regions.regions.putAll(entry.getKey(), entry.getValue());
				}
				return regions;
			},
			regions -> regions.regions.entries().stream().collect(Collectors.groupingBy(
					Map.Entry::getKey,
					HashMap::new,
					Collectors.mapping(Map.Entry::getValue, Collectors.toList())
			))
	);

	private final Multimap<String, BlockBox> regions = HashMultimap.create();

	public void add(String key, BlockBox region) {
		regions.put(key, region);
	}

	public void addAll(MapRegions regions) {
		this.regions.putAll(regions.regions);
	}

	public Set<String> keySet() {
		return regions.keySet();
	}

	public Collection<BlockBox> get(String key) {
		return regions.get(key);
	}

	@Nullable
	public BlockBox getAny(String key) {
		Collection<BlockBox> regions = this.regions.get(key);
		if (!regions.isEmpty()) {
			return regions.iterator().next();
		} else {
			return null;
		}
	}

	public List<BlockBox> getAll(String... keys) {
		return getAll(Arrays.asList(keys));
	}

	public List<BlockBox> getAll(Collection<String> keys) {
		return keys.stream().flatMap(key -> get(key).stream()).toList();
	}

	public List<BlockBox> getAllOrThrow(String key) {
		List<BlockBox> boxes = getAll(key);
		if (boxes.isEmpty()) {
			throw new GameException(Component.literal("Missing expected region with key '" + key + "'"));
		}
		return boxes;
	}

	public BlockBox getOrThrow(String key) {
		BlockBox box = getAny(key);
		if (box == null) {
			throw new GameException(Component.literal("Missing expected region with key '" + key + "'"));
		}
		return box;
	}

	public boolean isEmpty() {
		return regions.isEmpty();
	}
}
