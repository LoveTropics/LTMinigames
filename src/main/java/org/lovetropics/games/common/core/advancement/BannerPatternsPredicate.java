package org.lovetropics.games.common.core.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.CollectionPredicate;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.Optional;
import java.util.function.Predicate;

public record BannerPatternsPredicate(
		Optional<CollectionPredicate<BannerPatternLayers.Layer, LayerPredicate>> layers
) implements SingleComponentItemPredicate<BannerPatternLayers> {
	public static final Codec<BannerPatternsPredicate> CODEC = RecordCodecBuilder.create(i -> i.group(
			CollectionPredicate.codec(LayerPredicate.CODEC).optionalFieldOf("layers").forGetter(BannerPatternsPredicate::layers)
	).apply(i, BannerPatternsPredicate::new));

	@Override
	public DataComponentType<BannerPatternLayers> componentType() {
		return DataComponents.BANNER_PATTERNS;
	}

	@Override
	public boolean matches(BannerPatternLayers value) {
		return layers.isEmpty() || layers.get().test(value.layers());
	}

	public record LayerPredicate(Optional<HolderSet<BannerPattern>> pattern, Optional<DyeColor> color) implements Predicate<BannerPatternLayers.Layer> {
		public static final Codec<LayerPredicate> CODEC = RecordCodecBuilder.create(i -> i.group(
				RegistryCodecs.homogeneousList(Registries.BANNER_PATTERN).optionalFieldOf("pattern").forGetter(LayerPredicate::pattern),
				DyeColor.CODEC.optionalFieldOf("color").forGetter(LayerPredicate::color)
		).apply(i, LayerPredicate::new));

		@Override
		public boolean test(BannerPatternLayers.Layer layer) {
			return pattern.map(patterns -> patterns.contains(layer.pattern())).orElse(true)
					&& color.map(color -> color == layer.color()).orElse(true);
		}
	}
}
