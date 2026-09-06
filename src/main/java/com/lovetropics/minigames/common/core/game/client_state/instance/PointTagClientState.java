package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// TODO: consolidate all state types by using PartialUpdate system
public record PointTagClientState(ItemStackTemplate icon, Optional<String> translationKey, Map<UUID, Integer> points) implements GameClientState {
	public static final MapCodec<PointTagClientState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.SINGLE_STACK_TEMPLATE.fieldOf("item").forGetter(PointTagClientState::icon),
			Codec.STRING.optionalFieldOf("translation_key").forGetter(PointTagClientState::translationKey),
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).fieldOf("points").forGetter(PointTagClientState::points)
	).apply(i, PointTagClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.POINT_TAGS.get();
	}

	public @Nullable Component getPointsTextFor(final UUID id) {
		final Integer points = this.points.get(id);
		if (points == null) {
			return null;
		}
		if (translationKey.isPresent()) {
			return Component.translatable(translationKey.get(), points);
		} else {
			return Component.literal(String.valueOf(points));
		}
	}
}
