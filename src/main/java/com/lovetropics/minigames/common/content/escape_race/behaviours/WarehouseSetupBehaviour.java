package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceRoomsState;
import com.lovetropics.minigames.common.content.escape_race.misc.RoomEntrancePadEntity;
import com.lovetropics.minigames.common.content.escape_race.rooms.RoomStatus;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public record WarehouseSetupBehaviour(
		List<RoomConfig> rooms
) implements IGameBehavior {
	public static final MapCodec<WarehouseSetupBehaviour> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(RoomConfig.CODEC.listOf().fieldOf("rooms").forGetter(WarehouseSetupBehaviour::rooms)).apply(inst, WarehouseSetupBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		State state = new State();
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		events.listen(GamePhaseEvents.CREATE, () -> this.onGameStarted(game, state));
		events.listen(GamePhaseEvents.TICK, () -> {
			for (String roomEntranceRegion : state.rooms.keySet()) {
				RoomState roomState = state.rooms.get(roomEntranceRegion);
				BlockBox region = game.mapRegions().getOrThrow(roomEntranceRegion);
				for (GameTeamKey teamKey : teams.getTeamKeys()) {
					if (tickRoomForTeam(game, teamKey, roomState, teams, region)) {
						break;
					}
				}
			}
		});

		GameClientState.applyGlobally(game, events, SharedConstants.TICKS_PER_SECOND, EscapeRace.ROOMS_STATE.get(), player -> {
			GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
			return state.asClientState(teamForPlayer);
		});
	}

	private boolean tickRoomForTeam(IGamePhase game, GameTeamKey teamKey, RoomState roomState, TeamState teams, BlockBox region) {
		TeamRoomState teamRoomState = roomState.getTeamState(teamKey);
		if (teamRoomState.status == RoomStatus.LOCKED) {
			return tickLockedRoom(game, teamKey, teams, region, teamRoomState);
		}
		return false;
	}

	private boolean tickLockedRoom(IGamePhase game, GameTeamKey teamKey, TeamState teams, BlockBox region, TeamRoomState teamRoomState) {
		boolean allInArea = teams.getPlayersForTeam(game, teamKey)
				.stream().allMatch(t -> t.isCrouching() && region.contains(t.position()));
		if (allInArea) {
			teamRoomState.unlockingTicks++;
			if (teamRoomState.unlockingTicks >= SharedConstants.TICKS_PER_SECOND * 5) {
				teamRoomState.unlockingTicks = 0;
				teamRoomState.status = RoomStatus.UNLOCKED;
			}
			return true;
		} else if (teamRoomState.unlockingTicks > 0) {
			teamRoomState.unlockingTicks--;
		}
		return false;
	}

	private void onGameStarted(IGamePhase game, State state) {
		ServerLevel level = game.level();
		for (RoomConfig room : rooms) {
			RoomState roomState = new RoomState(room);
			BlockBox blockBoxes = game.mapRegions().getOrThrow(room.entranceRegion);
			RoomEntrancePadEntity pad = EscapeRace.ROOM_ENTRANCE_PAD.get().create(level, EntitySpawnReason.LOAD);
			if (pad == null) {
				throw new GameException(Component.literal("Could not spawn entrance pad"));
			}
			pad.snapTo(blockBoxes.center(), room.facing, 0.0f);
			BlockPos size = blockBoxes.size();
			pad.setWidth(size.getX() - 0.01f);
			pad.setHeight(size.getY());
			pad.setDepth(size.getZ() - 0.01f);
			roomState.setEntrancePad(pad);
			level.addFreshEntity(pad);
			state.rooms.put(room.entranceRegion, roomState);
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return EscapeRace.WAREHOUSE_SETUP_BEHAVIOUR;
	}

	public static class State {
		public Map<String, RoomState> rooms;
		public State() {
			this.rooms = new HashMap<>();
		}

		public EscapeRaceRoomsState asClientState(@Nullable GameTeamKey team) {
			Int2ObjectMap<EscapeRaceRoomsState.Room> rooms = new Int2ObjectOpenHashMap<>();
			for (RoomState room : this.rooms.values()) {
				RoomEntrancePadEntity pad = room.entrancePad;
				if (pad == null) {
					continue;
				}
				TeamRoomState teamState = team != null ? room.getTeamState(team) : null;
				rooms.put(pad.getId(), new EscapeRaceRoomsState.Room(
						room.config.displayName,
						// TODO: What do we show for spectators?
						teamState != null ? teamState.status : RoomStatus.LOCKED,
						room.config.cost
				));
			}
			return new EscapeRaceRoomsState(rooms);
		}
	}

	public record RoomConfig(
			String entranceRegion,
			float facing,
			int cost,
			Component displayName
	) {
		public static final Codec<RoomConfig> CODEC = RecordCodecBuilder.create(inst ->
				inst.group(Codec.STRING.fieldOf("entranceRegion").forGetter(RoomConfig::entranceRegion),
						Codec.FLOAT.fieldOf("facing").forGetter(RoomConfig::facing),
						Codec.INT.fieldOf("cost").forGetter(RoomConfig::cost),
						ComponentSerialization.CODEC.fieldOf("displayName").forGetter(RoomConfig::displayName)
				).apply(inst, RoomConfig::new));
	}

	public static class RoomState {
		public RoomConfig config;
		public final Map<GameTeamKey, TeamRoomState> teamStates = new HashMap<>();
		private @Nullable RoomEntrancePadEntity entrancePad;

		public RoomState(RoomConfig roomConfig) {
			this.config = roomConfig;
		}

		public TeamRoomState getTeamState(GameTeamKey teamKey) {
			return teamStates.computeIfAbsent(teamKey, k -> new TeamRoomState());
		}

		public RoomEntrancePadEntity getEntrancePad() {
			return Objects.requireNonNull(entrancePad);
		}

		public void setEntrancePad(RoomEntrancePadEntity entrancePad) {
			this.entrancePad = entrancePad;
		}
	}

	public static class TeamRoomState {
		private RoomStatus status = RoomStatus.LOCKED;
		private int unlockingTicks;
	}
}
