package org.lovetropics.games.common.content.bingo;

import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.util.Codecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Optional;
import java.util.function.Supplier;

/// @param clue    shown to players when they hover the tile while it is still locked, as a teaser of what it will be
/// @param trigger creates the actions that complete the tile, which should include `ltminigames:bingo/complete_tile`
public record BingoTileDefinition(ItemStackTemplate icon, Component title, Optional<Component> clue, int reward, Supplier<GameActionList> trigger) {
	public static final MapCodec<BingoTileDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(BingoTileDefinition::icon),
			ComponentSerialization.CODEC.fieldOf("title").forGetter(BingoTileDefinition::title),
			ComponentSerialization.CODEC.optionalFieldOf("clue").forGetter(BingoTileDefinition::clue),
			Codec.INT.fieldOf("reward").forGetter(BingoTileDefinition::reward),
			Codecs.newLazyCopies(GameActionList.CODEC).fieldOf("trigger").forGetter(BingoTileDefinition::trigger)
	).apply(in, BingoTileDefinition::new));
	public static final Codec<BingoTileDefinition> CODEC = MAP_CODEC.codec();
}
