package com.lovetropics.minigames.common.content.escape_race.client;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.misc.RoomEntrancePadEntity;
import com.lovetropics.minigames.common.content.escape_race.rooms.RoomStatus;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.util.Codecs;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

public record EscapeRaceRoomsState(
		Int2ObjectMap<Room> rooms
) implements GameClientState {
	// TODO: Allow client states generally to not support normal codecs
	public static final MapCodec<EscapeRaceRoomsState> CODEC = Codecs.no();
	public static final StreamCodec<RegistryFriendlyByteBuf, EscapeRaceRoomsState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(Int2ObjectOpenHashMap::new, ByteBufCodecs.VAR_INT, Room.STREAM_CODEC), EscapeRaceRoomsState::rooms,
			EscapeRaceRoomsState::new
	);

	public static final EscapeRaceRoomsState EMPTY = new EscapeRaceRoomsState(Int2ObjectMaps.emptyMap());

	public @Nullable Room byEntrance(RoomEntrancePadEntity entity) {
		return rooms.get(entity.getId());
	}

	@Override
	public GameClientStateType<?> getType() {
		return EscapeRace.ROOMS_STATE.get();
	}

	public record Room(
			Component name,
			RoomStatus status,
			int cost
	) {
		public static final Room EMPTY = new Room(
				CommonComponents.EMPTY,
				RoomStatus.LOCKED,
				0
		);

		public static final StreamCodec<RegistryFriendlyByteBuf, Room> STREAM_CODEC = StreamCodec.composite(
				ComponentSerialization.STREAM_CODEC, Room::name,
				RoomStatus.STREAM_CODEC, Room::status,
				ByteBufCodecs.VAR_INT, Room::cost,
				Room::new
		);
	}
}
