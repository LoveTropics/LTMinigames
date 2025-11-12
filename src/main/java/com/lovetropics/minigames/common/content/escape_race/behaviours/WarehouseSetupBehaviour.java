package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.misc.RoomEntrancePadEntity;
import com.lovetropics.minigames.common.content.escape_race.rooms.RoomStatus;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record WarehouseSetupBehaviour(
		List<RoomConfig> rooms
) implements IGameBehavior {
	public static final MapCodec<WarehouseSetupBehaviour> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(RoomConfig.CODEC.listOf().fieldOf("rooms").forGetter(WarehouseSetupBehaviour::rooms)).apply(inst, WarehouseSetupBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		State state = new State();
		events.listen(GamePhaseEvents.CREATE, () -> this.onGameStarted(game, state));
	}

	private void onGameStarted(IGamePhase game, State state) {
		ServerLevel level = game.level();
		for (RoomConfig room : rooms) {
			RoomState roomState = new RoomState(room);
			try {
				BlockBox blockBoxes = game.mapRegions().getOrThrow(room.entranceRegion);
				BlockPos blockPos = blockBoxes.centerBlock();
				RoomEntrancePadEntity pad = EscapeRace.ROOM_ENTRANCE_PAD.get().spawn(level, blockPos, EntitySpawnReason.LOAD);
				if(pad != null) {
					pad.setPos(blockBoxes.center());
					pad.setYRot(room.facing);
					BlockPos size = blockBoxes.size();
					pad.setWidth(size.getX() - 0.01f);
					pad.setHeight(size.getY());
					pad.setDepth(size.getZ() - 0.01f);
					roomState.setPadEntity(pad);
					pad.setCost(room.cost);
					pad.setRoomStatus(roomState.status);
					pad.setRoomName(room.displayName);
				}
			} catch (Exception ignored){}
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
		public RoomStatus status = RoomStatus.LOCKED;
		@Nullable
		public RoomEntrancePadEntity padEntity;

		public RoomState(RoomConfig roomConfig) {
			this.config = roomConfig;
		}

		public RoomStatus getStatus() {
			return status;
		}

		public void setStatus(RoomStatus status) {
			this.status = status;
			if(padEntity != null) {
				padEntity.setRoomStatus(status);
			}
		}

		@Nullable
		public RoomEntrancePadEntity getPadEntity() {
			return padEntity;
		}

		public void setPadEntity(@Nullable RoomEntrancePadEntity padEntity) {
			this.padEntity = padEntity;
		}
	}

}
