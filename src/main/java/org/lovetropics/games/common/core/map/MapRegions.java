package org.lovetropics.games.common.core.map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lovetropics.lib.BlockBox;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.lovetropics.games.common.core.game.GameRegions;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MapRegions implements GameRegions {
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

	@Override
	public Set<String> keySet() {
		return regions.keySet();
	}

	@Override
	public Collection<BlockBox> get(String key) {
		return regions.get(key);
	}

	public boolean isEmpty() {
		return regions.isEmpty();
	}
}
