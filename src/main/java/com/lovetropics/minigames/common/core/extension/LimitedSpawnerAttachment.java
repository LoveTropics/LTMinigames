package com.lovetropics.minigames.common.core.extension;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GameManager;
import com.lovetropics.minigames.mixin.BaseSpawnerAccessor;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID)
public class LimitedSpawnerAttachment {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LoveTropics.ID);

	public static final MapCodec<LimitedSpawnerAttachment> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("max_count").forGetter(a -> a.maxCount),
			UUIDUtil.CODEC_SET.optionalFieldOf("spawned_mobs", Set.of()).forGetter(a -> a.spawnedMobs)
	).apply(i, (maxCount, spawnedMobs) -> {
		LimitedSpawnerAttachment attachment = new LimitedSpawnerAttachment(maxCount);
		attachment.spawnedMobs.addAll(spawnedMobs);
		return attachment;
	}));

	public static final Supplier<AttachmentType<LimitedSpawnerAttachment>> ATTACHMENT = ATTACHMENT_TYPES.register(
			"limited_spawner",
			() -> AttachmentType.<LimitedSpawnerAttachment>builder(() -> {
				throw new IllegalStateException("Cannot create default limited_spawner attachment");
			}).serialize(MAP_CODEC).build()
	);

	private final int maxCount;
	private final Set<UUID> spawnedMobs = new HashSet<>();

	public LimitedSpawnerAttachment(int maxCount) {
		this.maxCount = maxCount;
	}

	@SubscribeEvent
	public static void onPreSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
		if (event.getSpawnType() != EntitySpawnReason.SPAWNER) {
			return;
		}
		ServerLevel level = event.getLevel().getLevel();
		BlockPos pos = event.getPos();
		if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity blockEntity) {
			blockEntity.getExistingData(ATTACHMENT).ifPresent(attachment ->
					((BaseSpawnerAccessor) blockEntity.getSpawner()).setMaxNearbyEntities(attachment.getMaxCount(level, pos))
			);
		}
	}

	@SubscribeEvent
	public static void onCheckSpawn(MobSpawnEvent.PositionCheck event) {
		BaseSpawner spawner = event.getSpawner();
		if (spawner == null || spawner.getOwner() == null) {
			return;
		}
		Either<BlockEntity, Entity> owner = spawner.getOwner();
		getAttachment(owner).ifPresent(attachment -> {
			ServerLevel level = event.getLevel().getLevel();
			attachment.onCheckSpawn(event, (BaseSpawnerAccessor) spawner, level, owner);
		});
	}

	@SubscribeEvent
	public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
		Either<BlockEntity, Entity> spawner = event.getSpawner();
		if (spawner != null) {
			getAttachment(spawner).ifPresent(attachment -> attachment.spawnedMobs.add(event.getEntity().getUUID()));
		}
	}

	private static Optional<LimitedSpawnerAttachment> getAttachment(Either<BlockEntity, Entity> spawner) {
		return spawner.map(
				blockEntity -> blockEntity.getExistingData(ATTACHMENT),
				entity -> entity.getExistingData(ATTACHMENT)
		);
	}

	private void onCheckSpawn(MobSpawnEvent.PositionCheck event, BaseSpawnerAccessor spawner, ServerLevel level, Either<BlockEntity, Entity> owner) {
		pruneDeadMobs(level);

		BlockPos pos = owner.map(BlockEntity::getBlockPos, Entity::blockPosition);
		int maxCount = getMaxCount(level, pos);
		if (spawnedMobs.size() >= maxCount) {
			spawner.invokeDelay(level, pos);
			event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
		}
	}

	private int getMaxCount(ServerLevel level, BlockPos pos) {
		IGamePhase game = GameManager.get().getGamePhaseAt(level, pos);
		if (game != null) {
			return game.invoker(RiverRaceEvents.MODIFY_MAX_SPAWN_COUNT).modifyMaxSpawnCount(pos, maxCount);
		}
		return maxCount;
	}

	private void pruneDeadMobs(ServerLevel level) {
		Iterator<UUID> iterator = spawnedMobs.iterator();
		while (iterator.hasNext()) {
			Entity mob = level.getEntity(iterator.next());
			if (mob == null || !mob.isAlive()) {
				iterator.remove();
			}
		}
	}
}
