package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public record ForcePerspectiveClientState(Perspective perspective) implements GameClientState {

	public static final MapCodec<ForcePerspectiveClientState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Perspective.CODEC.fieldOf("perspective").forGetter(ForcePerspectiveClientState::perspective)
	).apply(instance, ForcePerspectiveClientState::new));

	public static final StreamCodec<ByteBuf, ForcePerspectiveClientState> STREAM_CODEC = StreamCodec.composite(
			Perspective.STREAM_CODEC, ForcePerspectiveClientState::perspective,
			ForcePerspectiveClientState::new);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.FORCE_PERSPECTIVE.get();
	}

	public enum Perspective implements StringRepresentable {
		FIRST_PERSON(0, "first_person"),
		THIRD_PERSON_BACK(1, "third_person_back"),
		THIRD_PERSON_FRONT(2, "third_person_front");

		private static final Codec<Perspective> CODEC = StringRepresentable.fromEnum(Perspective::values);

		public static final StreamCodec<ByteBuf, Perspective> STREAM_CODEC = ByteBufCodecs.idMapper(
				ByIdMap.continuous(d -> d.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO),
				d -> d.id
		);

		private final int id;
		private final String name;

		Perspective(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}

}
