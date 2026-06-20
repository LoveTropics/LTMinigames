package com.lovetropics.minigames.common.core.integration.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.world.item.DyeColor;

import java.util.List;

public record DonationScale(
		// Scale of mob
		double scale,
		// Donation amount
		MinMaxBounds.Doubles amount,
		int color
) {
	public static final MapCodec<DonationScale> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.fieldOf("scale").forGetter(DonationScale::scale),
			MinMaxBounds.Doubles.CODEC.fieldOf("amount").forGetter(DonationScale::amount),
			Codec.INT.optionalFieldOf("color", DyeColor.WHITE.getTextColor()).forGetter(DonationScale::color)
	).apply(i, DonationScale::new));

	public static final Codec<DonationScale> CODEC = MAP_CODEC.codec();
	public static final Codec<List<DonationScale>> LIST_CODEC = DonationScale.CODEC.listOf();

	public static DonationScale getScale(final double testAmount, final List<DonationScale> scales) {
		for (final DonationScale scale : scales) {
			if (scale.amount.matches(testAmount)) {
				return scale;
			}
		}
		return new DonationScale(0, MinMaxBounds.Doubles.between(0, 1), DyeColor.WHITE.getTextColor());
	}
}
