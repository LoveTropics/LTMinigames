package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record ItemPutInItemFrameTrigger(
		Optional<EntityPredicate> itemFramePredicate,
		Optional<List<String>> itemFrameTags,
		Optional<ItemPredicate> itemPredicate,
		Optional<GameActionList<ServerPlayer>> matches,
		Optional<GameActionList<ServerPlayer>> doesntMatch,
		Optional<GameActionList<ServerPlayer>> empty
) implements IGameBehavior {

	public static final MapCodec<ItemPutInItemFrameTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(ItemPutInItemFrameTrigger::itemFramePredicate),
			Codec.STRING.listOf().optionalFieldOf("tags").forGetter(ItemPutInItemFrameTrigger::itemFrameTags),
			ItemPredicate.CODEC.optionalFieldOf("item_predicate").forGetter(ItemPutInItemFrameTrigger::itemPredicate),
			GameActionList.PLAYER_CODEC.optionalFieldOf("matches").forGetter(ItemPutInItemFrameTrigger::matches),
			GameActionList.PLAYER_CODEC.optionalFieldOf("doesnt_match").forGetter(ItemPutInItemFrameTrigger::doesntMatch),
			GameActionList.PLAYER_CODEC.optionalFieldOf("empty").forGetter(ItemPutInItemFrameTrigger::empty)
	).apply(i, ItemPutInItemFrameTrigger::new));


	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		matches.ifPresent(serverPlayerGameActionList -> serverPlayerGameActionList.register(game, events));
		doesntMatch.ifPresent(serverPlayerGameActionList -> serverPlayerGameActionList.register(game, events));
		empty.ifPresent(serverPlayerGameActionList -> serverPlayerGameActionList.register(game, events));
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> {
			if(target.getType() == EntityType.ITEM_FRAME) {
				ItemFrame itemFrame = (ItemFrame) target;
				boolean doCheckForItem = false;
				if (itemFramePredicate.isPresent()) {
					EntityPredicate entityPredicate = itemFramePredicate.get();
					if (entityPredicate.matches(player, target)) {
						doCheckForItem = true;
					}
				}
				if (itemFrameTags.isPresent()) {
					List<String> strings = itemFrameTags.get();
					for (String tagToCheck : strings) {
						if (itemFrame.getTags().contains(tagToCheck)) {
							doCheckForItem = true;
							break;
						}
					}
				}
				if(doCheckForItem) {
					if (!itemFrame.getItem().isEmpty()) {
						empty.ifPresent(serverPlayerGameActionList -> serverPlayerGameActionList.apply(game, ContextMap.EMPTY, player));
					}
				}
			}
			return TriState.DEFAULT;
		});
		events.listen(GamePlayerEvents.INTERACT_ENTITY, (player, target, hand) -> {
			if(target.getType() == EntityType.ITEM_FRAME){
				ItemFrame itemFrame = (ItemFrame) target;
				boolean doCheckForItem = false;
				if(itemFramePredicate.isPresent()){
					EntityPredicate entityPredicate = itemFramePredicate.get();
					if(entityPredicate.matches(player, target)) {
						doCheckForItem = true;
					}
				}
				if(itemFrameTags.isPresent()){
					List<String> strings = itemFrameTags.get();
					for (String tagToCheck : strings) {
						if(itemFrame.getTags().contains(tagToCheck)){
							doCheckForItem = true;
							break;
						}
					}
				}
				if(doCheckForItem) {
					ItemStack itemInHand = player.getItemInHand(hand);
					if (!itemInHand.isEmpty()) {
						if (itemFrame.getItem().isEmpty()) {
							if (itemPredicate.isPresent()) {
								if (itemPredicate.get().test(itemInHand)) {
									matches.ifPresent(serverPlayerGameActionList -> serverPlayerGameActionList.apply(game, ContextMap.EMPTY, player));
								} else {
									doesntMatch.ifPresent(serverPlayerGameActionList -> serverPlayerGameActionList.apply(game, ContextMap.EMPTY, player));
								}
							} else {
								matches.ifPresent(serverPlayerGameActionList -> serverPlayerGameActionList.apply(game, ContextMap.EMPTY, player));
							}
						}
					}
				}
			}
			return InteractionResult.PASS;
		});
	}
}
