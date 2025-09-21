package com.lovetropics.minigames.common.core.game.predicate.loot;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.MultiGameManager;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public class IsMinigameCondition implements LootItemCondition {
	public static final MapCodec<IsMinigameCondition> CODEC = RecordCodecBuilder.mapCodec(
			builder -> builder
					.group(
							ResourceLocation.CODEC.fieldOf("minigame_id").forGetter(idCondition -> idCondition.minigameId))
					.apply(builder, IsMinigameCondition::new));
	private final ResourceLocation minigameId;

	private IsMinigameCondition(final ResourceLocation minigameId) {
		this.minigameId = minigameId;
	}

	@Override
	public LootItemConditionType getType() {
		return LootItemConditions.IS_MINIGAME.value();
	}

	@Override
	public boolean test(LootContext lootContext) {
		IGamePhase phase = MultiGameManager.INSTANCE.getGamePhaseInDimension(lootContext.getLevel());
		if (phase == null) {
			return false;
		}
		return phase.definition().id().equals(minigameId);
	}
}
