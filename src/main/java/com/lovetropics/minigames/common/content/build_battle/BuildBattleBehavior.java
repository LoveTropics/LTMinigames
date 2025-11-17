package com.lovetropics.minigames.common.content.build_battle;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public final class BuildBattleBehavior implements IGameBehavior {
	private final String plotRegionsName;

	private static final Logger LOGGER = LogUtils.getLogger();

	private final HashMap<UUID, BlockBox> playerPlots = new HashMap<>();

	public static final MapCodec<BuildBattleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("plot_name").forGetter(c -> c.plotRegionsName)
	).apply(i, BuildBattleBehavior::new));

	public BuildBattleBehavior(String plotRegionsName) {
		this.plotRegionsName = plotRegionsName;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.START, initiator -> {
		});

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> assignPlots(participants, game.mapRegions().get(plotRegionsName)));
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(game.level(), playerId, spawn));

		// players cannot interact with blocks outside their plot
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> isInsidePlot(player.getUUID(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.PLACE_BLOCK, (player, pos, placed, placedOn, placedItemStack) -> isInsidePlot(player.getUUID(), pos)  ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, result) -> isInsidePlot(player.getUUID(), pos) ? InteractionResult.PASS : InteractionResult.FAIL);
	}

	public void assignPlots(Set<PlayerKey> players, Collection<BlockBox> plots) {
		if(plots.size() < players.size()) {
			throw new GameException(Component.literal("Not enough plots for all players in Build Battle game (" + plots.size() + " plots named \"" + plotRegionsName + "\" for " + players.size() + " players)"));
		}
		var iterator = players.iterator();
		for (BlockBox plot : plots) {
			if (!iterator.hasNext()) {
				break;
			}
			var player = iterator.next();
			playerPlots.put(player.id(), plot);
		}
	}

	public boolean isInsidePlot(UUID playerId, BlockPos pos) {
		return playerPlots.containsKey(playerId) && playerPlots.get(playerId).contains(pos);
	}

	private void spawnPlayer(ServerLevel level, UUID playerId, SpawnBuilder builder) {
		BlockBox region = playerPlots.get(playerId);
		if (region != null) {
			BlockPos pos = tryFindEmptyPos(level, level.getRandom(), region);
			builder.teleportTo(level, pos);
		}
	}

	private BlockPos tryFindEmptyPos(ServerLevel level, RandomSource random, BlockBox box) {
		for (int i = 0; i < 20; i++) {
			BlockPos pos = box.sample(random);
			if (level.isEmptyBlock(pos)) {
				return pos;
			}
		}
		LOGGER.debug("USING FALLBACK SPAWN POS");
		return box.centerBlock();
	}
}
