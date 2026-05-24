package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.Map;

public record SetGameRulesBehavior(boolean applyDefaults, GameRuleMap rules) implements IGameBehavior {
	public static final MapCodec<SetGameRulesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("apply_defaults", false).forGetter(SetGameRulesBehavior::applyDefaults),
			GameRuleMap.CODEC.fieldOf("rules").forGetter(c -> c.rules)
	).apply(i, SetGameRulesBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		final GameRules gameRules = game.level().getGameRules();

		if (applyDefaults) {
			GameRules defaultRules = new GameRules(FeatureFlags.VANILLA_SET);
			gameRules.setAll(defaultRules, null);
		}

		gameRules.setAll(rules, null);
	}
}
