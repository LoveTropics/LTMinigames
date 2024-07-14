package com.lovetropics.minigames.common.core.game.config;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorList;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorTemplate;
import com.lovetropics.minigames.common.core.game.map.GameMapProviders;
import com.lovetropics.minigames.common.core.game.map.IGameMapProvider;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.AABB;

import java.util.List;

public record GamePhaseConfig(IGameMapProvider map, AABB area, List<BehaviorTemplate> behaviors) implements IGamePhaseDefinition {
	public static final MapCodec<GamePhaseConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameMapProviders.CODEC.fieldOf("map").forGetter(c -> c.map),
			MoreCodecs.AABB.optionalFieldOf("area", Util.INFINITE_AABB).forGetter(c -> c.area),
			BehaviorTemplate.CODEC.listOf().fieldOf("behaviors").forGetter(c -> c.behaviors)
	).apply(i, GamePhaseConfig::new));
	public static final Codec<GamePhaseConfig> CODEC = MAP_CODEC.codec();

	@Override
	public IGameMapProvider getMap() {
		return map;
	}

	@Override
	public AABB getGameArea() {
		return area;
	}

	@Override
	public BehaviorList createBehaviors() {
		return BehaviorList.instantiate(behaviors);
	}
}
