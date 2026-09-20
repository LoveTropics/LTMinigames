package org.lovetropics.games.common.core.game.client_state.instance.controls;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

public record RemapHotbarKeysClientState(Map<Integer, Integer> remap) implements GameClientState {

	private static final Codec<Integer> FAKE_INT = Codec.STRING.xmap(Integer::parseInt, String::valueOf)
			.validate(i -> i < 1 || i > 9 ? DataResult.error(() -> "Key must be between 1 and 9") : DataResult.success(i));

	public static final MapCodec<RemapHotbarKeysClientState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.unboundedMap(FAKE_INT, FAKE_INT).fieldOf("remap").forGetter(RemapHotbarKeysClientState::remap)
	).apply(instance, RemapHotbarKeysClientState::new));

	public static final StreamCodec<ByteBuf, RemapHotbarKeysClientState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, ByteBufCodecs.INT), RemapHotbarKeysClientState::remap,
			RemapHotbarKeysClientState::new
	);

	public int getKeyFor(int key) {
		int index = key + 1;
		if (remap.containsKey(index)) {
			return remap.get(index) - 1;
		}
		return key;
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.REMAP_QUICK_KEYS.get();
	}
}
