package com.lovetropics.minigames.common.core.map.workspace;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

// TODO: Replace this with DataAttachment
@EventBusSubscriber(modid = LoveTropics.ID)
public final class WorkspacePositionTracker {
	private static final String NBT_KEY = LoveTropics.ID + ":map_workspace";
	private static final String NBT_RETURN_KEY = "return";

	public static void setPositionFor(ServerPlayer player, MapWorkspace workspace, Position position) {
		CompoundTag data = getOrCreateTag(player);
		data.put(workspace.id(), position.write());
	}

	@Nullable
	public static Position getPositionFor(ServerPlayer player, MapWorkspace workspace) {
		CompoundTag data = getOrCreateTag(player);
		return data.getCompound(workspace.id()).flatMap(Position::read).orElse(null);
	}

	public static void setReturnPositionFor(ServerPlayer player, Position position) {
		CompoundTag data = getOrCreateTag(player);
		data.put(NBT_RETURN_KEY, position.write());
	}

	@Nullable
	public static Position getReturnPositionFor(ServerPlayer player) {
		CompoundTag data = getOrCreateTag(player);
		return data.getCompound(NBT_RETURN_KEY).flatMap(Position::read).orElse(null);
	}

	private static CompoundTag getOrCreateTag(ServerPlayer player) {
		CompoundTag persistentData = player.getPersistentData();
		CompoundTag tag = persistentData.getCompoundOrEmpty(NBT_KEY);
		persistentData.put(NBT_KEY, tag);
		return tag;
	}

	@SubscribeEvent
	public static void onPlayerChangeDimension(EntityTravelToDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		MinecraftServer server = player.level().getServer();

		ResourceKey<Level> from = player.level().dimension();

		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(server);
		MapWorkspace fromWorkspace = workspaceManager.getWorkspace(from);

		WorkspacePositionTracker.Position position = WorkspacePositionTracker.Position.copyFrom(player);
		if (fromWorkspace != null) {
			WorkspacePositionTracker.setPositionFor(player, fromWorkspace, position);
		} else {
			if (isValidReturnDimension(server, from)) {
				WorkspacePositionTracker.setReturnPositionFor(player, position);
			}
		}
	}

	private static boolean isValidReturnDimension(MinecraftServer server, ResourceKey<Level> dimension) {
		RuntimeDimensions dimensions = RuntimeDimensions.get(server);
		return !dimensions.isTemporaryDimension(dimension);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		ServerPlayer fromPlayer = (ServerPlayer) event.getOriginal();
		ServerPlayer toPlayer = (ServerPlayer) event.getEntity();

		CompoundTag fromData = fromPlayer.getPersistentData();
		fromData.getCompound(NBT_KEY).ifPresent(fromTag ->
				toPlayer.getPersistentData().put(NBT_KEY, fromTag.copy())
		);
	}

	public record Position(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch) {
		public static final Codec<Position> CODEC = RecordCodecBuilder.create(i -> i.group(
				ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Position::dimension),
				Vec3.CODEC.fieldOf("position").forGetter(Position::pos),
				Codec.FLOAT.fieldOf("yaw").forGetter(Position::yaw),
				Codec.FLOAT.fieldOf("pitch").forGetter(Position::pitch)
		).apply(i, Position::new));

		public static Position copyFrom(ServerPlayer entity) {
			return new Position(entity.level().dimension(), entity.position(), entity.getYRot(), entity.getXRot());
		}

		public void applyTo(ServerPlayer entity) {
			ServerLevel level = entity.level().getServer().getLevel(dimension);
			entity.teleportTo(level, pos.x, pos.y, pos.z, Set.of(), yaw, pitch, false);
		}

		public Tag write() {
			return CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
		}

		public static Optional<Position> read(CompoundTag nbt) {
			return CODEC.parse(NbtOps.INSTANCE, nbt).result();
		}
	}
}
