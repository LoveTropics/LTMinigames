package org.lovetropics.games.common.core.game.client_state.instance.controls;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Input;

import java.util.HashMap;
import java.util.Map;

public record RemapMovementClientState(Map<MovementType, MovementType> keys) implements GameClientState {

	public static final MapCodec<RemapMovementClientState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.unboundedMap(MovementType.CODEC, MovementType.CODEC).fieldOf("keys").forGetter(RemapMovementClientState::keys)
	).apply(instance, RemapMovementClientState::new));

	public static final StreamCodec<ByteBuf, RemapMovementClientState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(HashMap::new, MovementType.STREAM_CODEC, MovementType.STREAM_CODEC), RemapMovementClientState::keys,
			RemapMovementClientState::new
	);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.REMAP_MOVEMENT.get();
	}

	public Input remap(Input original) {
		return new Input(
				getForType(original, MovementType.FORWARD),
				getForType(original, MovementType.BACKWARD),
				getForType(original, MovementType.LEFT),
				getForType(original, MovementType.RIGHT),
				getForType(original, MovementType.JUMP),
				getForType(original, MovementType.SHIFT),
				getForType(original, MovementType.SPRINT)
		);
	}

	private boolean getForType(Input original, MovementType type) {
		if (!keys.containsKey(type) && getInputForType(original, type)) {
			return true;
		}

		for (Map.Entry<MovementType, MovementType> entry : keys.entrySet()) {
			MovementType key = entry.getKey();
			MovementType value = entry.getValue();

			if (value == type && getInputForType(original, key)) {
				return true;
			}
		}
		return false;
	}

	private boolean getInputForType(Input original, MovementType type) {
		return switch (type) {
			case FORWARD -> original.forward();
			case BACKWARD -> original.backward();
			case LEFT -> original.left();
			case RIGHT -> original.right();
			case JUMP -> original.jump();
			case SHIFT -> original.shift();
			case SPRINT -> original.sprint();
			case DISABLED -> false;
		};
	}

	public enum MovementType implements StringRepresentable {
		FORWARD(0, "forward"),
		BACKWARD(1, "backward"),
		LEFT(2, "left"),
		RIGHT(3, "right"),
		JUMP(4, "jump"),
		SHIFT(5, "shift"),
		SPRINT(6, "sprint"),
		DISABLED(7, "disabled");

		private final int id;
		private final String name;

		private static final Codec<MovementType> CODEC = StringRepresentable.fromEnum(MovementType::values);
		public static final StreamCodec<ByteBuf, MovementType> STREAM_CODEC = ByteBufCodecs.idMapper(
				ByIdMap.continuous(d -> d.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO),
				d -> d.id
		);

		MovementType(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

}
