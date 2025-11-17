package com.lovetropics.minigames.common.content.build_battle;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public final class BuildBattleBehavior implements IGameBehavior {
	private final String plotRegionsName;
	private long buildTime;

	public static final MapCodec<BuildBattleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("plot_name").forGetter(c -> c.plotRegionsName),
			Codec.LONG.fieldOf("build_time").orElse((long) (5 * 60 * SharedConstants.TICKS_PER_SECOND)).forGetter(c -> c.buildTime)
	).apply(i, BuildBattleBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private final HashMap<UUID, BlockBox> playerPlots = new HashMap<>();
	private GameBossBar timer;
	private boolean building = true;

	public BuildBattleBehavior(String plotRegionsName, long buildTime) {
		this.plotRegionsName = plotRegionsName;
		this.buildTime = buildTime;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.START, initiator -> {
			timer = new GameBossBar(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
			game.allPlayers().forEach(timer::addPlayer);
			refreshBuildingTimeBar(0);
			building = true;
		});
		events.listen(GamePhaseEvents.DESTROY, () -> {
			timer.close();
		});
		events.listen(GamePhaseEvents.TICK, () -> {
			refreshBuildingTimeBar(game.ticks());
			if(game.ticks() == buildTime) {
				game.allPlayers().sendMessage(Component.literal("Building phase has ended! Time for the jury to review your wonderful creations.")); //TODO: translate
				building = false;
				timer.close();
			}
			if(game.ticks() == (buildTime + 5 * SharedConstants.TICKS_PER_SECOND)) {
				game.allPlayers().sendMessage(Component.literal("//TODO"));
			}
			if(game.ticks() == (buildTime + 10 * SharedConstants.TICKS_PER_SECOND)) {
				game.requestStop(GameStopReason.finished());
			}
		});

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> assignPlots(participants, game.mapRegions().get(plotRegionsName)));
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(game.level(), playerId, spawn));

		// players cannot interact with blocks outside their plot
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> canInteract(player.getUUID(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.PLACE_BLOCK, (player, pos, placed, placedOn, placedItemStack) -> canInteract(player.getUUID(), pos)  ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, result) -> canInteract(player.getUUID(), pos) ? InteractionResult.PASS : InteractionResult.FAIL);
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

	public boolean canInteract(UUID playerId, BlockPos pos) {
		return building && playerPlots.containsKey(playerId) && playerPlots.get(playerId).contains(pos);
	}

	private void spawnPlayer(ServerLevel level, UUID playerId, SpawnBuilder builder) {
		BlockBox region = playerPlots.getOrDefault(playerId, null);
		if (region != null) {
			builder.teleportTo(level, tryFindEmptyPos(level, level.getRandom(), region));
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

	private void refreshBuildingTimeBar(long ticks) {
		var remaining = buildTime - ticks;
		var minutes = (remaining / (60 * SharedConstants.TICKS_PER_SECOND)) % 60;
		var seconds = (remaining / SharedConstants.TICKS_PER_SECOND) % 60;
		//TODO: translate
		timer.setTitle(Component.literal("Building time! " + String.format("%02d:%02d", minutes, seconds) + " remaining"));
		timer.setProgress(buildTime > 0 ? (float) remaining / (float) buildTime : 0f);
	}
}
