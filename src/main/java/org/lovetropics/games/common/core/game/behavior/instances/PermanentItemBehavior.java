package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.ItemStackTemplate;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.ActionTarget;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.util.Codecs;

import java.util.Optional;

public record PermanentItemBehavior(
		WeightedList<ItemStackTemplate> item,
		int interval,
		Optional<Integer> maxCount,
		Optional<Integer> initialCount,
		ActionTarget target
) implements IGameBehavior {
	public static final MapCodec<PermanentItemBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codecs.compactNonEmptyWeightedList(ItemStackTemplate.CODEC).fieldOf("item").forGetter(c -> c.item),
			Codec.INT.optionalFieldOf("interval", 5).forGetter(c -> c.interval),
			Codec.INT.optionalFieldOf("max_count").forGetter(c -> c.maxCount),
			Codec.INT.optionalFieldOf("initial_count").forGetter(c -> c.initialCount),
			ActionTarget.CODEC.optionalFieldOf("target", ActionTarget.Simple.PARTICIPANTS).forGetter(c -> c.target)
	).apply(i, PermanentItemBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		if (initialCount.isPresent()) {
			int initialCount = this.initialCount.get();
			events.listen(GamePlayerEvents.SPAWN, (_, spawn, role) -> {
				if (role == PlayerRole.PARTICIPANT) {
					spawn.run(player -> {
						for (int i = 0; i < initialCount; i++) {
							player.getInventory().add(item.getRandomOrThrow(player.getRandom()).create());
						}
					});
				}
			});
		}
		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % interval != 0 || (initialCount.isPresent() && game.ticks() == 0)) {
				return;
			}
			ItemStackTemplate item = this.item.getRandomOrThrow(game.random());
			ActionSubjects<?> targets = target.resolveTargets(game, ActionSubjects.EMPTY);
			for (ServerPlayer player : targets.asPlayers(game)) {
				int currentCount = player.getInventory().countItem(item.item().value());
				int targetCount = maxCount.orElse(item.count());
				if (currentCount < targetCount) {
					int dropCount = Math.min(targetCount - currentCount, item.count());
					player.getInventory().add(item.create().copyWithCount(dropCount));
				}
			}
		});
	}
}
