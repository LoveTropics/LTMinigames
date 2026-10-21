package org.lovetropics.games.common.content.mcsr.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;

/// A rule of the game that gets stronger or weaker as its level changes, e.g. with donations.
/// What it does is up to the behaviors that read its level.
///
/// @param riddle    shown to players who haven't found out what the modifier is yet
/// @param baseLevel the level the game starts with, so a modifier can be on from the start
public record ModifierDefinition(String id, ItemStackTemplate icon, Component name, Component description, Component riddle, int baseLevel, int maxLevel) {
	public static final Codec<ModifierDefinition> CODEC = RecordCodecBuilder.<ModifierDefinition>create(in -> in.group(
			Codec.STRING.fieldOf("id").forGetter(ModifierDefinition::id),
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(ModifierDefinition::icon),
			ComponentSerialization.CODEC.fieldOf("name").forGetter(ModifierDefinition::name),
			ComponentSerialization.CODEC.fieldOf("description").forGetter(ModifierDefinition::description),
			ComponentSerialization.CODEC.fieldOf("riddle").forGetter(ModifierDefinition::riddle),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("base_level", 0).forGetter(ModifierDefinition::baseLevel),
			ExtraCodecs.POSITIVE_INT.fieldOf("max_level").forGetter(ModifierDefinition::maxLevel)
	).apply(in, ModifierDefinition::new)).validate(definition -> definition.baseLevel <= definition.maxLevel
			? DataResult.success(definition)
			: DataResult.error(() -> "Base level of modifier '" + definition.id + "' is above its max level"));

	public int clamp(int level) {
		return Math.clamp(level, 0, maxLevel);
	}
}
