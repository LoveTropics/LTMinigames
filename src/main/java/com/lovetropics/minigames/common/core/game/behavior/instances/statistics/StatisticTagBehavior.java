package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.PointTagClientState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record StatisticTagBehavior(StatisticKey<Integer> statistic, ItemStackTemplate icon) implements IGameBehavior {
	public static final MapCodec<StatisticTagBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(StatisticTagBehavior::statistic),
			MoreCodecs.SINGLE_STACK_TEMPLATE.fieldOf("icon").forGetter(StatisticTagBehavior::icon)
	).apply(i, StatisticTagBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		final Object2IntMap<UUID> points = new Object2IntOpenHashMap<>();

		events.listen(GamePlayerEvents.ADD, player -> GameClientState.sendToPlayer(createState(points), player));
		events.listen(GamePlayerEvents.REMOVE, player -> GameClientState.removeFromPlayer(GameClientStateTypes.POINT_TAGS.get(), player));

		events.listen(GamePhaseEvents.TICK, () -> {
			if (updateState(game, points)) {
				GameClientState.sendToPlayers(createState(points), game.allPlayers());
			}
		});
	}

	private boolean updateState(final IGamePhase game, final Object2IntMap<UUID> points) {
		boolean changed = false;
		for (ServerPlayer player : game.participants()) {
			final int value = game.statistics().forPlayer(player).getInt(statistic);
			if (points.put(player.getUUID(), value) != value) {
				changed = true;
			}
		}
		return changed;
	}

	private PointTagClientState createState(final Object2IntMap<UUID> points) {
		return new PointTagClientState(icon, Optional.empty(), points);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.STATISTIC_TAG;
	}
}
