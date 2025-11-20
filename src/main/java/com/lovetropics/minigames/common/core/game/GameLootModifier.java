package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

public record GameLootModifier() implements IGlobalLootModifier {
	public static final MapCodec<GameLootModifier> CODEC = MapCodec.unit(GameLootModifier::new);

	@Override
	public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
		IGamePhase game;
		if (entity != null) {
			game = GamePhaseManager.get().getGamePhaseFor(entity);
		} else {
			game = GamePhaseManager.get().getGamePhaseInDimension(context.getLevel());
		}
		if (game == null) {
			return generatedLoot;
		}
		return game.invoker(GameWorldEvents.MODIFY_LOOT_TABLE).modify(generatedLoot, context);
	}

	@Override
	public MapCodec<GameLootModifier> codec() {
		return CODEC;
	}
}
