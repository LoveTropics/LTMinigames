package org.lovetropics.games.common.core.game.client_state.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

import java.util.HashMap;
import java.util.Map;

public record RemapSpriteClientState(Map<Identifier, Identifier> remap) implements GameClientState {

	public static final MapCodec<RemapSpriteClientState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC).fieldOf("remap").forGetter(RemapSpriteClientState::remap)
	).apply(instance, RemapSpriteClientState::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, RemapSpriteClientState> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, Identifier.STREAM_CODEC), RemapSpriteClientState::remap,
			RemapSpriteClientState::new
	);

	public @Nullable Identifier getRemapTexture(Identifier texture) {
		if (!remap.containsKey(texture)) {
			return null;
		}

		return remap.get(texture);
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.REMAP_SPRITE.get();
	}
}
