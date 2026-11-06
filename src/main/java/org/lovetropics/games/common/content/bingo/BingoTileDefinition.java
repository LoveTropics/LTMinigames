package org.lovetropics.games.common.content.bingo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/// @param clue         shown to players when they hover the tile while it is still locked, as a teaser of what it will be
/// @param criteria     advancement criteria that complete the tile, like those of an advancement
/// @param requirements which of the criteria must be meet, like those of an advancement (all of them by default)
/// @param columnMultipliers if not empty, completing the tile multiplies the points of every other tile in its column for the player,
///                          including those already completed - by the multiplier of the place they completed it in, the last one for any place after
/// @param kind         icon
public record BingoTileDefinition(
		ItemStackTemplate icon,
		Component title,
		Optional<Component> description,
		Optional<Component> clue,
		int reward,
		Map<String, Criterion<?>> criteria,
		AdvancementRequirements requirements,
		BingoTileType tileType,
		BingoTileKind kind,
		List<Float> columnMultipliers
) {
	private static final Codec<Map<String, Criterion<?>>> CRITERIA_CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap(Codec.STRING, Criterion.CODEC));

	public static final MapCodec<BingoTileDefinition> MAP_CODEC = RecordCodecBuilder.<BingoTileDefinition>mapCodec(in -> in.group(
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(BingoTileDefinition::icon),
			ComponentSerialization.CODEC.fieldOf("title").forGetter(BingoTileDefinition::title),
			ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(BingoTileDefinition::description),
			ComponentSerialization.CODEC.optionalFieldOf("clue").forGetter(BingoTileDefinition::clue),
			Codec.INT.fieldOf("reward").forGetter(BingoTileDefinition::reward),
			CRITERIA_CODEC.fieldOf("criteria").forGetter(BingoTileDefinition::criteria),
			AdvancementRequirements.CODEC.optionalFieldOf("requirements").forGetter(d -> Optional.of(d.requirements())),
			BingoTileType.CODEC.optionalFieldOf("tile_type", BingoTileType.DEFAULT).forGetter(BingoTileDefinition::tileType),
			BingoTileKind.CODEC.optionalFieldOf("kind", BingoTileKind.OBTAIN).forGetter(BingoTileDefinition::kind),
			Codec.FLOAT.listOf().optionalFieldOf("column_multipliers", List.of()).forGetter(BingoTileDefinition::columnMultipliers)
	).apply(in, (icon, title, description, clue, reward, criteria, requirements, tileType, kind, columnMultipliers) -> new BingoTileDefinition(
			icon, title, description, clue, reward, criteria, requirements.orElseGet(() -> AdvancementRequirements.allOf(criteria.keySet())), tileType, kind, columnMultipliers
	))).validate(d -> d.requirements().validate(d.criteria().keySet()).map(_ -> d));
	public static final Codec<BingoTileDefinition> CODEC = MAP_CODEC.codec();
}
