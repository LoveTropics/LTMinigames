package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.google.common.collect.ImmutableList;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.util.ColliderSet;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

public record CollidersClientState(ColliderSet colliders) implements GameClientState {
	public static final MapCodec<CollidersClientState> CODEC = ColliderSet.CODEC.xmap(CollidersClientState::new, CollidersClientState::colliders);
	public static final StreamCodec<ByteBuf, CollidersClientState> STREAM_CODEC = ColliderSet.STREAM_CODEC.map(CollidersClientState::new, CollidersClientState::colliders);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.COLLIDERS.get();
	}

	public void addTo(AABB boundingBox, ImmutableList.Builder<VoxelShape> output) {
		colliders.addTo(boundingBox, output);
	}

	public @Nullable Vec3 clip(Vec3 start, Vec3 end) {
		return colliders.clip(start, end);
	}
}
