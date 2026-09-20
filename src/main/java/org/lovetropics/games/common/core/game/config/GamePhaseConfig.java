package org.lovetropics.games.common.core.game.config;

import org.lovetropics.games.common.core.game.behavior.BehaviorTemplate;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.map.GameMapProviders;
import org.lovetropics.games.common.core.game.map.IGameMapProvider;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GamePhaseConfig(IGameMapProvider map, BehaviorTemplate behaviors) {
	public static final MapCodec<GamePhaseConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameMapProviders.CODEC.fieldOf("map").forGetter(c -> c.map),
			BehaviorTemplate.CODEC.fieldOf("behaviors").forGetter(c -> c.behaviors)
	).apply(i, GamePhaseConfig::new));
	public static final Codec<GamePhaseConfig> CODEC = MAP_CODEC.codec();

	public IGameBehavior createBehavior() {
		return behaviors.instantiate();
	}
}
