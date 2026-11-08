package org.lovetropics.games.common.content.survive_the_tide.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.escape_race.EscapeRace;
import org.lovetropics.games.common.content.escape_race.misc.RoomEntrancePadEntity;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.GameWinner;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPeriod;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public record ExtractionLayerBehavior(
		RegionQuery layerRegion,
		int secondsForWin,
		int initialSecondsForWin,
		ProgressionPeriod unlockPeriod,
		ProgressChannel channel
) implements IGameBehavior {

	public static final MapCodec<ExtractionLayerBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("region").forGetter(c -> c.layerRegion),
			Codec.INT.fieldOf("seconds").forGetter(c -> c.secondsForWin),
			Codec.INT.fieldOf("initial_seconds").forGetter(c -> c.initialSecondsForWin),
			ProgressionPeriod.CODEC.fieldOf("unlock_period").forGetter(c -> c.unlockPeriod),
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(c -> c.channel)
	).apply(i, ExtractionLayerBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		State state = new State();
		Region actualLayerRegion = this.layerRegion.one(game);
		BooleanSupplier isUnlockTime = unlockPeriod.createPredicate(game, channel);
		TeamState teamState = game.instanceState()
				.getOrNull(TeamState.KEY);
		state.layerRegion = actualLayerRegion;
		events.listen(GameWorldEvents.CHUNK_LOAD, (level, chunk) -> {
			if(chunk.getPos().contains(actualLayerRegion.centerBlock())){
				state.pad = spawnPad(actualLayerRegion);
			}
		});
		events.listen(GamePhaseEvents.TICK, () -> tick(game, state, teamState, isUnlockTime));
	}

	private void tick(IGamePhase game, State state, TeamState teamState, BooleanSupplier isUnlockTime){
		if(state.pad == null || state.hasFinished){
			return;
		}
		if(!isUnlockTime.getAsBoolean()){
			updateActionBar(game, state, teamState);
			return;
		}
		if(!state.isActive){
			state.isActive = true;
			state.pad.setRoomName(Component.empty());
		}
		PlayerSet playersInRegion = game.participants().filter(serverPlayer -> state.layerRegion.contains(serverPlayer));
		if(playersInRegion.isEmpty()){
			if(state.unlockingTicks > 0) {
				state.unlockingTicks = 0;
				state.isUnlocking = false;
				state.isFirstUnlock = false;
				state.multipleTeams = false;
				state.winningTeam = null;
				state.countOfTeamInRegion = 0;
				state.pad.setUnlockingTicks(state.unlockingTicks, state.isUnlocking);
			}
			updateActionBar(game, state, teamState);
			return;
		}
		int actualSecondsForWin = state.isFirstUnlock ? initialSecondsForWin : secondsForWin;
		Set<GameTeamKey> teamsInRegion = playersInRegion.stream().map(serverPlayer -> teamState.getTeamForPlayer(serverPlayer)).collect(Collectors.toSet());
		Optional<GameTeamKey> winningTeam = teamsInRegion.stream().findFirst();
		if(teamsInRegion.size() > 1){
			state.unlockingTicks = 0;
			state.isUnlocking = false;
			state.isFirstUnlock = false;
			state.multipleTeams = true;
		} else if(teamsInRegion.size() == 1){
			state.multipleTeams = false;
			state.isFirstUnlock = false;
			winningTeam.ifPresent(
					(team) -> {
						PlayerSet playersForTeam = teamState.getParticipantsForTeam(game, team);
						if(playersForTeam.stream().allMatch(serverPlayer -> state.layerRegion.contains(serverPlayer))){
							state.unlockingTicks++;
							state.isUnlocking = true;
							state.winningTeam = team;
						}
						state.countOfTeamInRegion = playersForTeam.filter(serverPlayer -> state.layerRegion.contains(serverPlayer)).size();
					}
			);
		}
		state.pad.setUnlockingTicks(state.unlockingTicks, state.isUnlocking);
		updateActionBar(game, state, teamState);
		if(state.unlockingTicks >= actualSecondsForWin * SharedConstants.TICKS_PER_SECOND){
			state.hasFinished = true;
			winningTeam.ifPresent(gameTeamKey -> {
				game.invoker(GameLogicEvents.GAME_OVER).onGameOver(new GameWinner.Team(teamState
						.getTeamByKey(gameTeamKey)));
			});
		}
	}

	private void updateActionBar(IGamePhase gamePhase, State state, TeamState teamState){
		int actualSecondsForWin = state.isFirstUnlock ? initialSecondsForWin : secondsForWin;
		Component teamName = Component.literal("No Team");
		Component playerCount = Component.literal("0 of 0 players");
		Component timer = Component.literal(state.unlockingTicks / SharedConstants.TICKS_PER_SECOND + "s / " + actualSecondsForWin + "s");
		if(state.winningTeam != null){
			teamName = teamState.getTeamByKey(state.winningTeam).styledName();
			playerCount = Component.literal(state.countOfTeamInRegion + " of " + teamState.getParticipantsForTeam(gamePhase, state.winningTeam).size() + " players");
		}
		MutableComponent totalActionBar = Component.empty();
		if(state.isActive) {
			if (state.multipleTeams) {
				totalActionBar = totalActionBar.append("Multiple teams in extraction zone").append(" - ").append(timer);
			} else {
				totalActionBar = totalActionBar.append(teamName).append(" - ").append(playerCount).append(" - ").append(timer);
			}
		} else {
			totalActionBar = totalActionBar.append("The Extraction Zone is not open.");
		}
		MutableComponent finalTotalActionBar = totalActionBar;
		if(state.isActive) {
			gamePhase.allPlayers().forEach(serverPlayer -> serverPlayer.sendOverlayMessage(finalTotalActionBar));
		} else {
			gamePhase.allPlayers().filter(player -> state.layerRegion.contains(player))
					.forEach(serverPlayer -> serverPlayer.sendOverlayMessage(finalTotalActionBar));
		}
	}

	public RoomEntrancePadEntity spawnPad(Region region){
		RoomEntrancePadEntity pad = EscapeRace.ROOM_ENTRANCE_PAD.get().create(region.level(), EntitySpawnReason.LOAD);
		if (pad == null) {
			throw new GameException(Component.literal("Could not spawn entrance pad"));
		}
		Vec3 center = region.center();
		Vec3i size = region.size();
		pad.snapTo(center.x(), region.min().getY(), center.z(), 0.0f, 0.0f);
		pad.setWidth(size.getX() - 0.01f);
		pad.setHeight(size.getY());
		pad.setDepth(size.getZ() - 0.01f);
		region.level().getChunkSource().updateChunkForced(pad.chunkPosition(), true);
		region.level().addFreshEntity(pad);
		pad.setRoomName(Component.literal("Extraction Zone - Closed."));
		return pad;
	}

	public static class State {
		public @Nullable RoomEntrancePadEntity pad;
		public boolean isActive = false;
		public Region layerRegion;
		public boolean isFirstUnlock = true;
		public boolean isUnlocking = false;
		public boolean hasFinished = false;
		public boolean multipleTeams = false;
		public int unlockingTicks = 0;
		public @Nullable GameTeamKey winningTeam;
		public int countOfTeamInRegion = 0;
	}
}
