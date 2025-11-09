package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;

public final class BlockInRegionTrigger implements IGameBehavior {

	public static final MapCodec<BlockInRegionTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(BlockInRegionTrigger::region),
			BlockPredicate.CODEC.fieldOf("predicate").forGetter(BlockInRegionTrigger::predicate),
			Codec.BOOL.optionalFieldOf("all_match", true).forGetter(BlockInRegionTrigger::allMatch),
			GameActionList.VOID_CODEC.fieldOf("actions").forGetter(BlockInRegionTrigger::actions),
			Codec.BOOL.optionalFieldOf("run_once", true).forGetter(BlockInRegionTrigger::runOnce)
	).apply(i, BlockInRegionTrigger::new));

	private final String region;
	private final BlockPredicate predicate;
	private final boolean allMatch;
	private final GameActionList<Void> actions;
	private final boolean runOnce;

	private boolean triggered = false;

	public BlockInRegionTrigger(String region, BlockPredicate predicate, boolean allMatch, GameActionList<Void> actions, boolean runOnce) {
		this.region = region;
		this.predicate = predicate;
		this.allMatch = allMatch;
		this.actions = actions;
		this.runOnce = runOnce;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox blockRegion = game.mapRegions().getOrThrow(region);
		actions.register(game, events);

		events.listen(GamePhaseEvents.TICK, () -> {
			for (BlockPos pos : blockRegion) {
				boolean matches = predicate.matches(game.level(), pos);
				if (allMatch && !matches) {
					return;
				} else if (!allMatch && matches) {
					tryRunActions(game);
					return;
				}
			}
			if (allMatch) {
				tryRunActions(game);
			}
		});
	}

	private void tryRunActions(IGamePhase game) {
		if (runOnce && triggered) {
			return;
		}
		actions.apply(game, GameActionContext.EMPTY);
		triggered = true;
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.BLOCK_IN_REGION;
	}

	public String region() {
		return region;
	}

	public BlockPredicate predicate() {
		return predicate;
	}

	public boolean allMatch() {
		return allMatch;
	}

	public GameActionList<Void> actions() {
		return actions;
	}

	public boolean runOnce() {
		return runOnce;
	}
}
