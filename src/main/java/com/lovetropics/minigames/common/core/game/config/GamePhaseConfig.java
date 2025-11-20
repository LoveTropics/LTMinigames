package com.lovetropics.minigames.common.core.game.config;

import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorTemplate;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.map.GameMapProviders;
import com.lovetropics.minigames.common.core.game.map.IGameMapProvider;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GamePhaseConfig(IGameMapProvider map, BehaviorTemplate behaviors) implements IGamePhaseDefinition {
	public static final MapCodec<GamePhaseConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameMapProviders.CODEC.fieldOf("map").forGetter(c -> c.map),
			BehaviorTemplate.CODEC.fieldOf("behaviors").forGetter(c -> c.behaviors)
	).apply(i, GamePhaseConfig::new));
	public static final Codec<GamePhaseConfig> CODEC = MAP_CODEC.codec();

	@Override
	public IGameMapProvider getMap() {
		return map;
	}

	@Override
	public IGameBehavior createBehavior() {
		return behaviors.instantiate();
	}
}
