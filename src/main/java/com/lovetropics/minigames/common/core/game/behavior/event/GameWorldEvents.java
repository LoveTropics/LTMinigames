package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.core.game.weather.WeatherEvent;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.HitResult;

import org.jspecify.annotations.Nullable;
import java.util.List;

public final class GameWorldEvents {
	public static final GameEventType<ChunkLoad> CHUNK_LOAD = GameEventType.create(ChunkLoad.class, listeners -> (chunk) -> {
		for (ChunkLoad listener : listeners) {
			listener.onChunkLoad(chunk);
		}
	});

	public static final GameEventType<ExplosionSound> EXPLOSION_SOUND = GameEventType.create(ExplosionSound.class, listeners -> (explosion, sound) -> {
		for (ExplosionSound listener : listeners) {
			sound = listener.updateExplosionSound(explosion, sound);
		}
		return sound;
	});

	public static final GameEventType<ExplosionDetonate> EXPLOSION_DETONATE = GameEventType.create(ExplosionDetonate.class, listeners -> (explosion, affectedBlocks, affectedEntities) -> {
		for (ExplosionDetonate listener : listeners) {
			listener.onExplosionDetonate(explosion, affectedBlocks, affectedEntities);
		}
	});

	public static final GameEventType<SaplingGrow> SAPLING_GROW = GameEventType.create(SaplingGrow.class, listeners -> (world, pos) -> {
		for (SaplingGrow listener : listeners) {
			TriState result = listener.onSaplingGrow(world, pos);
			if (!result.isDefault()) {
				return result;
			}
		}

		return TriState.DEFAULT;
	});

	public static final GameEventType<CropGrow> CROP_GROW = GameEventType.create(CropGrow.class, listeners -> (world, pos) -> {
		for (CropGrow listener : listeners) {
			TriState result = listener.onCropGrow(world, pos);
			if (!result.isDefault()) {
				return result;
			}
		}

		return TriState.DEFAULT;
	});

	public static final GameEventType<SetWeather> SET_WEATHER = GameEventType.create(SetWeather.class, listeners -> (lastEvent, event) -> {
		for (SetWeather listener : listeners) {
			listener.onSetWeather(lastEvent, event);
		}
	});

	public static final GameEventType<BlockLanded> BLOCK_LANDED = GameEventType.create(BlockLanded.class, listeners -> (level, pos, state) -> {
		for (var listener : listeners) {
			listener.onBlockLanded(level, pos, state);
		}
	});

	public static final GameEventType<BlockDrops> BLOCK_DROPS = GameEventType.create(BlockDrops.class, listeners -> (player, pos, blockState, blockEntity, tool, drops) -> {
		for (BlockDrops listener : listeners) {
			listener.updateBlockDrops(player, pos, blockState, blockEntity, tool, drops);
		}
	});

	public static final GameEventType<EntityAdded> ENTITY_ADDED = GameEventType.create(EntityAdded.class, listeners -> (entity) -> {
		for (EntityAdded listener : listeners) {
			listener.onEntityAdded(entity);
		}
	});

	public static final GameEventType<EntityRemoved> ENTITY_REMOVED = GameEventType.create(EntityRemoved.class, listeners -> (entity) -> {
		for (EntityRemoved listener : listeners) {
			listener.onEntityRemoved(entity);
		}
	});

	public static final GameEventType<ProjectileImpact> PROJECTILE_IMPACT = GameEventType.create(ProjectileImpact.class, listeners -> (projectile, hitResult) -> {
		for (ProjectileImpact listener : listeners) {
			listener.onProjectileImpact(projectile, hitResult);
		}
	});

	public static final GameEventType<SpawnPlacementCheck> SPAWN_PLACEMENT_CHECK = GameEventType.create(SpawnPlacementCheck.class, listeners -> (pos, reason, entityType) -> {
		for (SpawnPlacementCheck listener : listeners) {
			TriState result = listener.canSpawn(pos, reason, entityType);
			if (!result.isDefault()) {
				return result;
			}
		}
		return TriState.DEFAULT;
	});

	public static final GameEventType<TrialSpawnerEjectLoot> TRIAL_SPAWNER_EJECT_LOOT = GameEventType.create(TrialSpawnerEjectLoot.class, listeners -> (pos, trialSpawner) -> {
		for (TrialSpawnerEjectLoot listener : listeners) {
			if (listener.onTrialSpawnerEjectLoot(pos, trialSpawner)) {
				return true;
			}
		}
		return false;
	});

	public static final GameEventType<ModifyLootTable> MODIFY_LOOT_TABLE = GameEventType.create(ModifyLootTable.class, listeners -> (generatedLoot, context) -> {
		for (ModifyLootTable listener : listeners) {
			generatedLoot = listener.modify(generatedLoot, context);
		}
		return generatedLoot;
	});

	public static final GameEventType<TrapdoorToggle> TRAPDOOR_TOGGLE = GameEventType.create(TrapdoorToggle.class, listeners -> (level, pos, blockState) -> {
		for (TrapdoorToggle listener : listeners) {
			TriState result = listener.onTrapDoorToggle(level, pos, blockState);
			if (!result.isDefault()) {
				return result;
			}
		}
		return TriState.DEFAULT;
	});

	private GameWorldEvents() {
	}

	public interface ChunkLoad {
		void onChunkLoad(ChunkAccess chunk);
	}

	public interface ExplosionSound {
		Holder<SoundEvent> updateExplosionSound(Explosion explosion, Holder<SoundEvent> sound);
	}

	public interface ExplosionDetonate {
		void onExplosionDetonate(Explosion explosion, List<BlockPos> affectedBlocks, List<Entity> affectedEntities);
	}

	public interface SaplingGrow {
		TriState onSaplingGrow(Level world, BlockPos pos);
	}

	public interface CropGrow {
		TriState onCropGrow(Level world, BlockPos pos);
	}

	public interface SetWeather {
		void onSetWeather(@Nullable WeatherEvent lastEvent, @Nullable WeatherEvent event);
	}

	public interface BlockLanded {
		void onBlockLanded(ServerLevel level, BlockPos pos, BlockState state);
	}

	public interface BlockDrops {
		void updateBlockDrops(ServerPlayer player, BlockPos pos, BlockState blockState, @Nullable BlockEntity blockEntity, ItemStack tool, List<ItemEntity> drops);
	}

	public interface EntityAdded {
		void onEntityAdded(Entity entity);
	}

	public interface EntityRemoved {
		void onEntityRemoved(Entity entity);
	}

	public interface ProjectileImpact {
		void onProjectileImpact(Projectile projectile, HitResult result);
	}

	public interface SpawnPlacementCheck {
		TriState canSpawn(BlockPos pos, EntitySpawnReason reason, EntityType<?> entityType);
	}

	public interface TrialSpawnerEjectLoot {
		boolean onTrialSpawnerEjectLoot(BlockPos pos, TrialSpawner trialSpawner);
	}

	public interface ModifyLootTable {
		ObjectArrayList<ItemStack> modify(ObjectArrayList<ItemStack> generatedLoot, LootContext context);
	}

	public interface TrapdoorToggle {
		TriState onTrapDoorToggle(ServerLevel world, BlockPos pos, BlockState state);
	}
}
