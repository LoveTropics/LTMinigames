package com.lovetropics.minigames.common.content.drr_showdown;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.event.EscapeRaceEvents;
import com.lovetropics.minigames.common.content.turtle_race.RiderBehavior;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

public class DDRShowdownBehavior implements IGameBehavior {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Component NO_DDR_LEVEL = Component.literal("Unable to get random DDR level.");

	public static final MapCodec<DDRShowdownBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			RegistryCodecs.homogeneousList(EscapeRace.DDR_LEVEL).fieldOf("levels").forGetter(ddrShowdownBehavior -> ddrShowdownBehavior.allowedLevels),
			Codec.STRING.optionalFieldOf("ddr_spawn_region", "ddr_spawn").forGetter(ddrShowdownBehavior -> ddrShowdownBehavior.ddrSpawnRegion),
			Codec.STRING.optionalFieldOf("spectator_spawn_region", "spectators").forGetter(ddrShowdownBehavior -> ddrShowdownBehavior.spectatorSpawnRegion)
	).apply(instance, DDRShowdownBehavior::new));

	private final String spectatorSpawnRegion;
	private final String ddrSpawnRegion;
	private final HolderSet<DdrLevel> allowedLevels;

	public DDRShowdownBehavior(HolderSet<DdrLevel> allowedLevels, String ddrSpawnRegion, String spectatorSpawnRegion) {
		this.allowedLevels = allowedLevels;
		this.ddrSpawnRegion = ddrSpawnRegion;
		this.spectatorSpawnRegion = spectatorSpawnRegion;
	}

	private final List<DDRMachineEntity> activeMachines = new ArrayList<>();

	private boolean gameMarkedToEnd = false;
	private GameWidgets widgets;
	private GameBossBar bossBar;
	private long startTick;
	private int gameLength;


	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameStatistics statistics = game.statistics();
		widgets = GameWidgets.getOrRegister(game, events);
		Holder<DdrLevel> level = getRandomLevel(game);
		DdrLevel ddrLevel = level.value();
		gameLength = Math.toIntExact(level.value().lastRecordedTick());
		bossBar = widgets.openGlobalBossBar(ddrLevel.displayName(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
		bossBar.setProgress(0f);

		MapRegions regions = game.mapRegions();
		List<BlockBox> ddrSpawn = regions.get(ddrSpawnRegion).stream().toList();
		Queue<DDRMachineEntity> spawnedMachines = new ArrayDeque<>();

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> {
			int participantCount = participants.size();
			int locationToSpawnAt = Math.min(participantCount, ddrSpawn.size());
			for (int i = 0; i < locationToSpawnAt; i++) {
				BlockBox spawnRegion = ddrSpawn.get(i);
				DDRMachineEntity ddrMachineEntity = EscapeRace.DDR_MACHINE.get().create(game.level(), EntitySpawnReason.COMMAND);
				if (ddrMachineEntity != null) {
					ddrMachineEntity.setPos(spawnRegion.centerBlock().getX(), spawnRegion.centerBlock().getY(), spawnRegion.centerBlock().getZ());
					ddrMachineEntity.setDisplayLevels(false);
					game.level().addFreshEntity(ddrMachineEntity);
					spawnedMachines.add(ddrMachineEntity);
				}
			}
		});

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (role == PlayerRole.PARTICIPANT) {
				DDRMachineEntity poll = spawnedMachines.poll();
				if (poll == null) {
					LOGGER.warn("Could not DDR Machine for {}", playerId);
					spawn.teleportTo(game.level(), game.mapRegions().getOrThrow(spectatorSpawnRegion).centerBlock());
					return;
				}
				spawn.run(player -> {
					player.startRiding(poll, true, true);
					player.setData(RiderBehavior.FORCE_RIDER, true);
				});
				activeMachines.add(poll);
				return;
			}
			spawn.teleportTo(game.level(), game.mapRegions().getOrThrow(spectatorSpawnRegion).centerBlock());
		});

		// Todo clean this part up
		events.listen(GamePhaseEvents.START, initiator -> {
			game.scheduler().runAfterSeconds(7, () -> startGame(game, level));
			game.scheduler().runAfterSeconds(6, () -> title(game, "Dance!"));
			game.scheduler().runAfterSeconds(5, () -> title(game, "1!"));
			game.scheduler().runAfterSeconds(4, () -> title(game, "2!"));
			game.scheduler().runAfterSeconds(3, () -> title(game, "3!"));
			game.scheduler().runAfterSeconds(2, () -> title(game, "4!"));
			game.scheduler().runAfterSeconds(1, () -> title(game, "5!"));
		});


		events.listen(EscapeRaceEvents.DDR_LEVEL_COMPLETED, (player, _, score, _) -> statistics.forPlayer(player).set(StatisticKey.DDR_SCORE, score));
		events.listen(GamePhaseEvents.TICK, () -> onTick(game));
		events.listen(GameLogicEvents.GAME_OVER, winner -> onGameWinner(game, winner));
	}

	private void startGame(IGamePhase game, Holder<DdrLevel> level) {
		startTick = game.ticks();
		activeMachines.forEach(machine -> {
			LivingEntity controllingPassenger = machine.getControllingPassenger();
			if (controllingPassenger instanceof ServerPlayer player) {
				machine.startPlaying(player, level);
			}
		});
	}

	private void onTick(IGamePhase game) {
		if (gameMarkedToEnd) {
			return;
		}
		GameStatistics statistics = game.statistics();
		float progress = (float) (game.ticks() - startTick) / (float) gameLength;
		bossBar.setProgress(progress);

		if (progress >= 1) {
			awardWinner(game);
			gameMarkedToEnd = true;
			return;
		}

		activeMachines.forEach(machine -> {
			LivingEntity controllingPassenger = machine.getControllingPassenger();
			if (controllingPassenger instanceof ServerPlayer player) {
				statistics.forPlayer(player).set(StatisticKey.DDR_SCORE, machine.getCurrentScore());
			}
		});
	}

	private void awardWinner(IGamePhase game) {
		for (ServerPlayer player : game.allPlayers()) {
			player.setData(RiderBehavior.FORCE_RIDER, false);
		}
		activeMachines.forEach(Entity::discard);

		// Todo Players could tie but like...
		GameStatistics statistics = game.statistics();
		PlayerKey playerKey = statistics.getPlayers().stream().max((o1, o2) -> {
			int score1 = statistics.forPlayer(o1).getInt(StatisticKey.DDR_SCORE);
			int score2 = statistics.forPlayer(o2).getInt(StatisticKey.DDR_SCORE);
			return Integer.compare(score1, score2);
		}).orElse(null);

		GameWinner winner = playerKey != null ? GameWinner.byPlayerKey(game, playerKey) : new GameWinner.Nobody();
		game.invoker(GameLogicEvents.GAME_OVER).onGameOver(winner);
	}

	private void onGameWinner(IGamePhase game, GameWinner winner) {
		game.allPlayers().showTitle(winner.name(), DDRShowdownTexts.WINNER, 0, SharedConstants.TICKS_PER_SECOND * 3, 20);
		for (ServerPlayer player : winner.resolveSubjects(game).asPlayers(game)) {
			AttributeInstance attribute = player.getAttribute(Attributes.SCALE);
			if (attribute != null) {
				attribute.setBaseValue(5f);
			}
			player.setGlowingTag(true);
		}
	}


	private void title(IGamePhase game, String title) {
		game.allPlayers().showTitle(Component.literal(title), Component.empty(), 0, 20, 0);
	}


	private Holder<DdrLevel> getRandomLevel(IGamePhase game) throws GameException {
		return allowedLevels.getRandomElement(game.random())
				.orElseThrow(() -> new GameException(NO_DDR_LEVEL));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return DDRShowdown.DDR_SHOWDOWN;
	}
}
