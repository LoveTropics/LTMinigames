package com.lovetropics.minigames.common.core.game.predicate.loot;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

// TODO: Replace usages of this with modify_loot_tables behavior and remove
public class IsMinigameCondition implements LootItemCondition {
	public static final MapCodec<IsMinigameCondition> CODEC = RecordCodecBuilder.mapCodec(
			builder -> builder
					.group(
							Identifier.CODEC.fieldOf("minigame_id").forGetter(idCondition -> idCondition.minigameId))
					.apply(builder, IsMinigameCondition::new));
	private final Identifier minigameId;

	private IsMinigameCondition(final Identifier minigameId) {
		this.minigameId = minigameId;
	}

	@Override
	public boolean test(LootContext lootContext) {
		IGamePhase phase = GamePhaseManager.get().getGamePhaseInDimension(lootContext.getLevel());
		if (phase == null) {
			return false;
		}
		return phase.definition().id().equals(minigameId);
	}

	@Override
	public MapCodec<? extends LootItemCondition> codec() {
		return CODEC;
	}
}
