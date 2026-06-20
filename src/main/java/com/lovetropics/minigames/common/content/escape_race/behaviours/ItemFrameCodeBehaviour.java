package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import com.lovetropics.minigames.common.content.survive_the_tide.block.BigRedButtonBlock;
import com.lovetropics.minigames.common.content.survive_the_tide.block.BigRedButtonBlockEntity;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.mixin.ItemFrameMixin;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ItemFrameCodeBehaviour(
		List<CodeItemFrame> itemFrames,
		String buttonRegion,
		GameActionList correctCode,
		int lockoutTicks,
		GameActionList incorrectCode,
		Optional<GameActionList> lockoutCleared
) implements IGameBehavior {

	public static final MapCodec<ItemFrameCodeBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			CodeItemFrame.CODEC.listOf().fieldOf("item_frames").forGetter(ItemFrameCodeBehaviour::itemFrames),
			Codec.STRING.fieldOf("button_region").forGetter(ItemFrameCodeBehaviour::buttonRegion),
			GameActionList.CODEC.fieldOf("correct_code").forGetter(ItemFrameCodeBehaviour::correctCode),
			Codec.INT.fieldOf("lockout_ticks").forGetter(ItemFrameCodeBehaviour::lockoutTicks),
			GameActionList.CODEC.fieldOf("incorrect_code").forGetter(ItemFrameCodeBehaviour::incorrectCode),
			GameActionList.CODEC.optionalFieldOf("lockout_cleared").forGetter(ItemFrameCodeBehaviour::lockoutCleared)
	).apply(i, ItemFrameCodeBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		State state = new State();
		correctCode.register(game, events);
		incorrectCode.register(game, events);
		lockoutCleared.ifPresent(gameActionList -> gameActionList.register(game, events));
		Map<BlockBox, CodeItemFrame> regionToItemFrame = new HashMap<>();
		for (CodeItemFrame itemFrame : itemFrames) {
			regionToItemFrame.put(game.mapRegions().getOrThrow(itemFrame.itemFrameRegion()), itemFrame);
		}
		BlockBox submitButton = game.mapRegions().getOrThrow(buttonRegion);
		events.listen(GamePlayerEvents.USE_BLOCK, (player, world, pos, hand, traceResult) -> {
			if(submitButton.contains(pos)){
				BlockState blockState = world.getBlockState(pos);
				if(blockState.is(SurviveTheTide.BIG_RED_BUTTON) && !blockState.getValue(BigRedButtonBlock.TRIGGERED)){
					BlockEntity blockEntity = world.getBlockEntity(pos);
					if(blockEntity instanceof BigRedButtonBlockEntity bigRedButtonBlockEntity){
						bigRedButtonBlockEntity.trigger();
					}
					if(isWrongCode(game, regionToItemFrame, world, player)){
						state.lockoutStartTicks = game.ticks();
					}
					return InteractionResult.PASS;
				}
			}
			return InteractionResult.PASS;
		});
		events.listen(GamePhaseEvents.TICK, () -> {
			if(state.lockoutStartTicks > -1 && (game.ticks() >= state.lockoutStartTicks + lockoutTicks)) {
				state.lockoutStartTicks = -1;
				BlockPos pos = submitButton.centerBlock();
				ServerLevel level = game.level();
				BlockEntity blockEntity = level.getBlockEntity(pos);
				if(blockEntity instanceof BigRedButtonBlockEntity bigRedButtonBlockEntity){
					bigRedButtonBlockEntity.reset();
				}
				level.setBlock(pos, level.getBlockState(pos).setValue(BigRedButtonBlock.TRIGGERED, false)
						.setValue(BigRedButtonBlock.POWERED, false), BigRedButtonBlock.UPDATE_CLIENTS);
				lockoutCleared.ifPresent(gameActionList -> gameActionList.apply(game, ContextMap.EMPTY));
			}
		});
	}

	private boolean isWrongCode(IGamePhase game, Map<BlockBox, CodeItemFrame> itemFrameMap, ServerLevel level, ServerPlayer player){
		boolean isWrong = false;
		for (BlockBox blockPos : itemFrameMap.keySet()) {
			CodeItemFrame itemFrame = itemFrameMap.get(blockPos);
			List<ItemFrame> itemFramesInRegion = level.getEntitiesOfClass(ItemFrame.class, blockPos.asAabb());
			if(!itemFramesInRegion.isEmpty()){
				ItemFrame first = itemFramesInRegion.getFirst();
				if(!first.getItem().isEmpty()){
					if(!itemFrame.itemPredicate.test(first.getItem())){
						isWrong = true;
						((ItemFrameMixin) first).setFixed(false);
						first.setItem(ItemStack.EMPTY);
					} else {
						((ItemFrameMixin) first).setFixed(true);
						if(itemFrame.lightRegion().isPresent()){
							BlockBox itemFrameLight = game.mapRegions().getOrThrow(itemFrame.lightRegion().get());
							BlockPos lightPos = itemFrameLight.centerBlock();
							level.setBlock(lightPos, level.getBlockState(lightPos).setValue(CopperBulbBlock.LIT, true), BigRedButtonBlock.UPDATE_CLIENTS);
						}
					}
				} else {
					isWrong = true;
					break;
				}
			}
		}
		if(!isWrong){
			// It's correct!
			correctCode.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
		} else {
			incorrectCode.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
		}
		return isWrong;
	}

	public record CodeItemFrame(String itemFrameRegion, Optional<String> lightRegion, ItemPredicate itemPredicate) {
		public static final Codec<CodeItemFrame> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("item_frame_region").forGetter(CodeItemFrame::itemFrameRegion),
				Codec.STRING.optionalFieldOf("light_region").forGetter(CodeItemFrame::lightRegion),
				ItemPredicate.CODEC.fieldOf("item_predicate").forGetter(CodeItemFrame::itemPredicate)
		).apply(i, CodeItemFrame::new));
	}

	private static class State {
		public long lockoutStartTicks = -1;
	}
}
