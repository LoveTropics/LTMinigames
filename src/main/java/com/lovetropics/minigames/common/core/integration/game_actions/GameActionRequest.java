package com.lovetropics.minigames.common.core.integration.game_actions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;

import java.time.Instant;
import java.util.UUID;

public record GameActionRequest(GameActionType type, UUID uuid, Instant time, GameAction action) {
	public static MapCodec<GameActionRequest> codec(GameActionType type, MapCodec<GameAction> codec) {
		return RecordCodecBuilder.mapCodec(i -> i.group(
				UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(GameActionRequest::uuid),
				ExtraCodecs.INSTANT_ISO8601.fieldOf(type.getTimeFieldName()).forGetter(GameActionRequest::time),
				codec.forGetter(GameActionRequest::action)
		).apply(i, (uuid, triggerTime, action) -> new GameActionRequest(type, uuid, triggerTime, action)));
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof GameActionRequest request && uuid.equals(request.uuid());
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
}
