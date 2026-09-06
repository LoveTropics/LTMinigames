package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import org.jspecify.annotations.Nullable;
import java.util.Map;

public record ReplaceTexturesClientState(Map<TextureType, Identifier> textures) implements GameClientState {
	public static final MapCodec<ReplaceTexturesClientState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(TextureType.CODEC, Identifier.CODEC).fieldOf("textures").forGetter(c -> c.textures)
	).apply(i, ReplaceTexturesClientState::new));

	public @Nullable Identifier getTexture(TextureType type) {
		return textures.get(type);
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.REPLACE_TEXTURES.get();
	}

	public enum TextureType implements StringRepresentable {
		HOTBAR("hotbar"),
		;

		public static final Codec<TextureType> CODEC = MoreCodecs.stringVariants(values(), TextureType::getSerializedName);

		private final String key;

		TextureType(String key) {
			this.key = key;
		}

		@Override
		public String getSerializedName() {
			return key;
		}
	}
}
