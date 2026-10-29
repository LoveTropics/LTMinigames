package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.util.context.ContextMap;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public final class BlockInRegionTrigger implements IGameBehavior {

	public static final MapCodec<BlockInRegionTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("region").forGetter(BlockInRegionTrigger::region),
			BlockPredicate.CODEC.fieldOf("predicate").forGetter(BlockInRegionTrigger::predicate),
			Codec.BOOL.optionalFieldOf("all_match", true).forGetter(BlockInRegionTrigger::allMatch),
			GameActionList.MAP_CODEC.forGetter(BlockInRegionTrigger::actions),
			Codec.BOOL.optionalFieldOf("run_once", true).forGetter(BlockInRegionTrigger::runOnce)
	).apply(i, BlockInRegionTrigger::new));

	private final RegionQuery region;
	private final BlockPredicate predicate;
	private final boolean allMatch;
	private final GameActionList actions;
	private final boolean runOnce;

	private boolean triggered = false;

	public BlockInRegionTrigger(RegionQuery region, BlockPredicate predicate, boolean allMatch, GameActionList actions, boolean runOnce) {
		this.region = region;
		this.predicate = predicate;
		this.allMatch = allMatch;
		this.actions = actions;
		this.runOnce = runOnce;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);

		events.listen(GamePhaseEvents.TICK, () -> {
			MutableBoolean shouldRun = new MutableBoolean();
			region.forEachBlock(game, (level, pos) -> {
				if (shouldRun.isTrue()) {
					return;
				}
				boolean matches = predicate.matches(level, pos);
				if (allMatch && !matches) {
					shouldRun.setTrue();
				} else if (!allMatch && matches) {
					shouldRun.setTrue();
				}
			});
			if (shouldRun.isTrue()) {
				tryRunActions(game);
			}
		});
	}

	private void tryRunActions(IGamePhase game) {
		if (runOnce && triggered) {
			return;
		}
		actions.apply(game, ContextMap.EMPTY);
		triggered = true;
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}

	public RegionQuery region() {
		return region;
	}

	public BlockPredicate predicate() {
		return predicate;
	}

	public boolean allMatch() {
		return allMatch;
	}

	public GameActionList actions() {
		return actions;
	}

	public boolean runOnce() {
		return runOnce;
	}
}
