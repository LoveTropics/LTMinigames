package com.lovetropics.minigames.common.content.dance_off;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.event.EscapeRaceEvents;
import com.lovetropics.minigames.common.content.turtle_race.RiderBehavior;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
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
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DanceOffBehavior implements IGameBehavior {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<DanceOffBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			RegistryCodecs.homogeneousList(EscapeRace.DDR_LEVEL).fieldOf("levels").forGetter(danceOffBehavior -> danceOffBehavior.allowedLevels),
			Codec.STRING.optionalFieldOf("ddr_spawn_region", "ddr_spawn").forGetter(danceOffBehavior -> danceOffBehavior.ddrSpawnRegion),
			Codec.STRING.optionalFieldOf("spectator_spawn_region", "spectators").forGetter(danceOffBehavior -> danceOffBehavior.spectatorSpawnRegion)
	).apply(instance, DanceOffBehavior::new));

	private final String spectatorSpawnRegion;
	private final String ddrSpawnRegion;
	private final HolderSet<DdrLevel> allowedLevels;

	public DanceOffBehavior(HolderSet<DdrLevel> allowedLevels, String ddrSpawnRegion, String spectatorSpawnRegion) {
		this.allowedLevels = allowedLevels;
		this.ddrSpawnRegion = ddrSpawnRegion;
		this.spectatorSpawnRegion = spectatorSpawnRegion;
	}

	private final Map<UUID, Integer> playerScores = new HashMap<>();
	private final List<DDRMachineEntity> activeMachines = new ArrayList<>();
	public boolean shouldStopGame = false;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		MapRegions regions = game.mapRegions();
		List<BlockBox> ddrSpawn = regions.get(this.ddrSpawnRegion).stream().toList();
		Queue<DDRMachineEntity> spawnedMachines = new ArrayDeque<>();
		Holder<DdrLevel> level = allowedLevels.getRandomElement(game.random()).orElseThrow(() -> new GameException(Component.literal("No DDR levels available for this game")));

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
				DDRMachineEntity poll = spawnedMachines.peek();
				if (poll == null) {
					LOGGER.warn("Didn't find spawn for {} as {}", playerId, role);
					spawn.teleportTo(game.level(), game.mapRegions().getOrThrow(this.spectatorSpawnRegion).centerBlock());
					return;
				}
				spawn.run(player -> {
					player.startRiding(poll, true, true);
					player.setData(RiderBehavior.FORCE_RIDER, true);
				});
				activeMachines.add(poll);
				return;
			}
			spawn.teleportTo(game.level(), game.mapRegions().getOrThrow(this.spectatorSpawnRegion).centerBlock());
		});

		events.listen(GamePhaseEvents.START, initiator -> {
			game.scheduler().runAfterSeconds(7, () -> startGame(game, level));
			game.scheduler().runAfterSeconds(6, () -> title(game, "Dance!"));
			game.scheduler().runAfterSeconds(5, () -> title(game, "1!"));
			game.scheduler().runAfterSeconds(4, () -> title(game, "2!"));
			game.scheduler().runAfterSeconds(3, () -> title(game, "3!"));
			game.scheduler().runAfterSeconds(2, () -> title(game, "4!"));
			game.scheduler().runAfterSeconds(1, () -> title(game, "5!"));
		});

		events.listen(EscapeRaceEvents.DDR_LEVEL_COMPLETED, (player, _, score, _) -> playerScores.put(player.getUUID(), score));
		events.listen(GamePhaseEvents.TICK, () -> {
			if (shouldStopGame) {
				game.requestStop(GameStopReason.finished());
			}
		});
	}

	private void title(IGamePhase game, String title) {
		game.allPlayers().showTitle(Component.literal(title), Component.empty(), 0, 20, 0);
	}

	private void startGame(IGamePhase game, Holder<DdrLevel> level) {
		DdrLevel value = level.value();
		activeMachines.forEach(machine -> {
			LivingEntity controllingPassenger = machine.getControllingPassenger();
			if (controllingPassenger instanceof ServerPlayer player) {
				machine.startPlaying(player, level);
			}
		});
		int timeToEnd = Math.toIntExact(value.lastRecordedTick() + (SharedConstants.TICKS_PER_SECOND * 5));
		game.scheduler().runAfterTicks(timeToEnd, () -> endGame(game));
	}

	private void endGame(IGamePhase game) {
		// Because the game might end mid-song, for stop everyone
		activeMachines.forEach(ddrMachineEntity -> {
			LivingEntity controllingPassenger = ddrMachineEntity.getControllingPassenger();
			if (controllingPassenger instanceof ServerPlayer player) {
				ddrMachineEntity.stopPlaying(player);
			}
		});
		int highestScore = playerScores.values().stream().max(Integer::compareTo).orElse(0);
		List<ServerPlayer> winners = new ArrayList<>();
		playerScores.forEach((uuid, playerScore) -> {
			if (playerScore == highestScore) {
				ServerPlayer winner = game.level().getServer().getPlayerList().getPlayer(uuid);
				if (winner != null) {
					winners.add(winner);
				}
			}
		});
		String names = winners.stream()
				.map(ServerPlayer::nameAndId)
				.map(NameAndId::name)
				.collect(Collectors.joining(", "));

		Component winnerMessage = Component.literal("Winner(s): " + names);
		game.allPlayers().sendMessage(winnerMessage, false);
		this.shouldStopGame = true;
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return DanceOff.DANCE_OFF;
	}
}
