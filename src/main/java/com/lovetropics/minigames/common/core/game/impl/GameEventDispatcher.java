package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.IGameLookup;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.PickUpResult;
import com.lovetropics.minigames.mixin.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class GameEventDispatcher {
	public static GameEventDispatcher instance;

	private final IGameLookup gameLookup;
	private final Queue<LoadedChunk> loadedChunksQueue = new ConcurrentLinkedQueue<>();

	public GameEventDispatcher(IGameLookup gameLookup) {
		this.gameLookup = gameLookup;
		instance = this;
	}

	@SubscribeEvent
	public void onChunkLoad(ChunkEvent.Load event) {
		if (event.getLevel() instanceof ServerLevelAccessor levelAccessor) {
			ChunkAccess chunk = event.getChunk();
			ResourceKey<Level> dimension = levelAccessor.getLevel().dimension();
			loadedChunksQueue.add(new LoadedChunk(dimension, chunk.getPos()));
		}
	}

	// Bit hacky, would be nice to rethink - ensures that we run on the server thread, but also only after the game is initialised
	@SubscribeEvent
	public void onServerTickEnd(ServerTickEvent.Post event) {
		LoadedChunk queuedChunk;
		ResourceKey<Level> lastDimension = null;
		IGamePhase game = null;
		while ((queuedChunk = loadedChunksQueue.poll()) != null) {
			if (lastDimension != queuedChunk.dimension) {
				ServerLevel level = event.getServer().getLevel(queuedChunk.dimension);
				lastDimension = queuedChunk.dimension;
				game = level != null ? gameLookup.getGamePhaseInDimension(level) : null;
			}
			if (game != null) {
				LevelChunk chunk = game.level().getChunk(queuedChunk.pos.x(), queuedChunk.pos.z());
				game.invoker(GameWorldEvents.CHUNK_LOAD).onChunkLoad(chunk);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerDamaged(LivingDamageEvent.Pre event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			IGamePhase game = gameLookup.getGamePhaseFor(player);
			if (game != null) {
				try {
					DamageSource source = event.getSource();
					float amount = event.getNewDamage();
					TriState result = game.invoker(GamePlayerEvents.DAMAGE).onDamage(player, source, amount);
					if (result.isFalse()) {
						event.setNewDamage(0.0f);
						return;
					}
					float newAmount = game.invoker(GamePlayerEvents.DAMAGE_AMOUNT).getDamageAmount(player, source, amount, amount);
					if (newAmount != amount) {
						event.setNewDamage(newAmount);
					}
				} catch (Exception e) {
					LoveTropics.LOGGER.warn("Failed to dispatch player hurt event", e);
				}
			}
		}
	}

	@SubscribeEvent
	public void onAttackEntity(AttackEntityEvent event) {
		Entity target = event.getTarget();

		Player player = event.getEntity();
		IGamePhase game = gameLookup.getGamePhaseFor(player);
		if (game != null) {
			if (dispatchAttackEvent(game, (ServerPlayer) player, target)) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onDamageEntityIndirect(LivingIncomingDamageEvent event) {
		Entity target = event.getEntity();
		DamageSource source = event.getSource();

		IGamePhase game = gameLookup.getGamePhaseFor(target);
		if (game != null && !source.isDirect() && source.getEntity() instanceof ServerPlayer indirectSource) {
			if (dispatchAttackEvent(game, indirectSource, target)) {
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerAttack(LivingIncomingDamageEvent event) {
		Entity target = event.getEntity();
		DamageSource source = event.getSource();

		IGamePhase game = gameLookup.getGamePhaseFor(target);
		if (game != null && source.getEntity() instanceof ServerPlayer indirectSource) {

			if (dispatchAttackDamageEvent(game, indirectSource, target, event.getAmount())) {
				event.setCanceled(true);
			}
		}
	}

	private boolean dispatchAttackEvent(IGamePhase game, ServerPlayer player, Entity target) {
		try {
			TriState result = game.invoker(GamePlayerEvents.ATTACK).onAttack(player, target);
			return result.isFalse();
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Failed to dispatch player attack event", e);
		}
		return false;
	}

	private boolean dispatchAttackDamageEvent(IGamePhase game, ServerPlayer player, Entity target, float amount) {
		try {
			TriState result = game.invoker(GamePlayerEvents.ATTACK_DAMAGE).onAttackDamage(player, target, amount);
			return result.isFalse();
		} catch (Exception e) {
			LoveTropics.LOGGER.warn("Failed to dispatch player attack event", e);
		}
		return false;
	}

	@SubscribeEvent
	public void onLivingUpdate(EntityTickEvent.Post event) {
		if (event.getEntity() instanceof LivingEntity entity) {
			IGamePhase game = gameLookup.getGamePhaseFor(entity);
			if (game != null) {
				if (entity instanceof ServerPlayer && game.participants().contains(entity)) {
					try {
						game.invoker(GamePlayerEvents.TICK).tick((ServerPlayer) entity);
					} catch (Exception e) {
						LoveTropics.LOGGER.warn("Failed to dispatch player tick event", e);
					}
				}

				try {
					game.invoker(GameLivingEntityEvents.TICK).tick(entity);
				} catch (Exception e) {
					LoveTropics.LOGGER.warn("Failed to dispatch living tick event", e);
				}
			}
		}
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();

		IGamePhase game = gameLookup.getGamePhaseFor(entity);
		if (game == null) {
			return;
		}

		if (entity instanceof ServerPlayer player) {
			try {
				TriState result = game.invoker(GamePlayerEvents.DEATH).onDeath(player, event.getSource());
				// There's never case that we want to allow players to go through the normal death process
				event.setCanceled(true);

				if (entity.isDeadOrDying()) {
					entity.setHealth(entity.getMaxHealth());
				}
				entity.setDeltaMovement(Vec3.ZERO);
				entity.fallDistance = 0.0f;
				// If the entity was in lava, they are no longer in lava - please stop burning me :)
				if (entity instanceof EntityAccessor entityAccessor) {
//					entityAccessor.invokeUpdateInWaterStateAndDoFluidPushing(); // Todo 26.1 Port
				}

				if (!result.isFalse()) {
					// Respawn was not handled, just kick from game
					game.returnToParent(player);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player death event", e);
			}
		} else {
			try {
				game.invoker(GameLivingEntityEvents.DEATH).onDeath(entity, event.getSource());
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch entity death event", e);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerCraft(PlayerEvent.ItemCraftedEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			IGamePhase game = gameLookup.getGamePhaseFor(player);
			if (game != null) {
				game.invoker(GamePlayerEvents.CRAFT).onCraft(player, event.getCrafting(), event.getInventory());
			}
		}
	}

	@SubscribeEvent
	public void onMobDrop(LivingDropsEvent event) {
		LivingEntity entity = event.getEntity();

		IGamePhase game = gameLookup.getGamePhaseFor(entity);
		if (game != null) {
			try {
				TriState result = game.invoker(GameLivingEntityEvents.MOB_DROP).onMobDrop(entity, event.getSource(), event.getDrops());
				if (result.isFalse()) {
					event.setCanceled(true);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch entity mob drop event", e);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		IGamePhase game = gameLookup.getGamePhaseFor(event.getEntity());
		if (game != null) {
			try {
				game.invoker(GamePlayerEvents.RESPAWN).onRespawn((ServerPlayer) event.getEntity());
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player respawn event", e);
			}
		}
	}

	@SubscribeEvent
	public void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
		IGamePhase game = gameLookup.getGamePhaseFor(event.getEntity());
		if (game != null) {
			try {
				TriState result = game.invoker(GameLivingEntityEvents.FARMLAND_TRAMPLE).onFarmlandTrample(event.getEntity(), event.getPos(), event.getState());
				if (result.isFalse()) {
					event.setCanceled(true);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch farmland trample event", e);
			}
		}
	}

	@SubscribeEvent
	public void onEnderTeleport(EntityTeleportEvent.EnderPearl event) {
		IGamePhase game = gameLookup.getGamePhaseFor(event.getEntity());
		if (game != null) {
			try {
				game.invoker(GameLivingEntityEvents.ENDER_PEARL_TELEPORT).onEnderPearlTeleport(event.getPlayer(), event.getTargetX(), event.getTargetY(), event.getTargetZ(), event.getAttackDamage(), event::setAttackDamage);
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch ender teleport event", e);
			}
		}
	}

	public boolean onPlayerThrowItem(ServerPlayer player, ItemEntity item) {
		IGamePhase game = gameLookup.getGamePhaseFor(player);
		if (game != null) {
			try {
				TriState result = game.invoker(GamePlayerEvents.THROW_ITEM).onThrowItem(player, item);
				if (result.isFalse()) {
					player.getInventory().add(item.getItem());
					player.containerMenu.broadcastFullState();
					return true;
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player throw item event", e);
			}
		}
		return false;
	}

	@SubscribeEvent
	public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
		IGamePhase game = gameLookup.getGamePhaseFor(event.getEntity());
		if (game != null) {
			ServerPlayer player = (ServerPlayer) event.getEntity();

			try {
				InteractionResult result = game.invoker(GamePlayerEvents.INTERACT_ENTITY).onInteractEntity(player, event.getTarget(), event.getHand());
				if (result.consumesAction()) {
					event.setCancellationResult(result);
					event.setCanceled(true);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player interact entity event", e);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		IGamePhase game = gameLookup.getGamePhaseFor(event.getEntity());
		if (game != null) {
			ServerPlayer player = (ServerPlayer) event.getEntity();

			try {
				game.invoker(GamePlayerEvents.LEFT_CLICK_BLOCK).onLeftClickBlock(player, player.level(), event.getPos());
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player left click block event", e);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		IGamePhase game = gameLookup.getGamePhaseFor(event.getEntity());
		if (game != null) {
			ServerPlayer player = (ServerPlayer) event.getEntity();

			try {
				InteractionResult blockResult = game.invoker(GamePlayerEvents.USE_BLOCK).onUseBlock(player, player.level(), event.getPos(), event.getHand(), event.getHitVec());
				if (blockResult == InteractionResult.CONSUME) {
					event.setUseBlock(TriState.FALSE);
				} else if (blockResult.consumesAction()) {
					event.setCanceled(true);
					event.setCancellationResult(blockResult);
					return;
				}
				InteractionResult itemResult = game.invoker(GamePlayerEvents.USE_ITEM_ON_BLOCK).onUseBlock(player, player.level(), event.getPos(), event.getHand(), event.getHitVec());
				if (itemResult == InteractionResult.CONSUME) {
					event.setUseItem(TriState.FALSE);
				} else if (itemResult.consumesAction()) {
					event.setCanceled(true);
					event.setCancellationResult(itemResult);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player use block event", e);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
		IGamePhase game = gameLookup.getGamePhaseFor(event.getEntity());
		if (game != null && event.getEntity() instanceof ServerPlayer player) {
			try {
				InteractionResult result = game.invoker(GamePlayerEvents.USE_ITEM).onUseItem(player, event.getHand());
				if (result.consumesAction()) {
					event.setCancellationResult(result);
					event.setCanceled(true);
					resendPlayerHeldItem(player, event.getHand());
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player item use event", e);
			}
		}
	}

	@SubscribeEvent
	public void onPlayerBreakBlock(BreakBlockEvent event) {
		IGamePhase game = gameLookup.getGamePhaseFor(event.getPlayer());
		if (game != null && event.getPlayer() instanceof ServerPlayer player) {
			try {
				InteractionHand hand = player.getUsedItemHand();
				TriState result = game.invoker(GamePlayerEvents.BREAK_BLOCK).onBreakBlock(player, event.getPos(), event.getState(), hand);
				if (result.isFalse()) {
					event.setCanceled(true);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player break block event", e);
			}
		}
	}

	private static final ThreadLocal<ItemStack> placedItemStack = new ThreadLocal<>();

	public static void capturePlacedItemStack(ItemStack itemStack) {
		placedItemStack.set(itemStack);
	}

	public static void clearPlacedItemStack() {
		placedItemStack.remove();
	}

	@SubscribeEvent
	public void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event) {
		ItemStack placedItemStack = Objects.requireNonNullElse(GameEventDispatcher.placedItemStack.get(), ItemStack.EMPTY);
		clearPlacedItemStack();

		if (event.getEntity() instanceof ServerPlayer player) {
			IGamePhase game = gameLookup.getGamePhaseFor(player);
			if (game == null) {
				return;
			}
			try {
				TriState result = game.invoker(GamePlayerEvents.PLACE_BLOCK).onPlaceBlock(player, event.getPos(), event.getPlacedBlock(), event.getPlacedAgainst(), placedItemStack);
				if (result.isFalse()) {
					event.setCanceled(true);
					resendPlayerHeldItem(player, InteractionHand.MAIN_HAND);
					resendPlayerHeldItem(player, InteractionHand.OFF_HAND);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch player place block event", e);
			}
		}
	}

	public static void resendPlayerHeldItem(ServerPlayer player, InteractionHand hand) {
		int handSlot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : Inventory.SLOT_OFFHAND;
		ItemStack handItem = player.getItemInHand(hand);
		InventoryMenu inventory = player.inventoryMenu;
		player.connection.send(new ClientboundContainerSetSlotPacket(inventory.containerId, inventory.incrementStateId(), handSlot, handItem));
	}

	public Holder<SoundEvent> modifyExplosionSound(ServerLevel level, ServerExplosion explosion, Holder<SoundEvent> sound) {
		IGamePhase game = gameLookup.getGamePhaseAt(level, BlockPos.containing(explosion.center()));
		if (game != null) {
			try {
				return game.invoker(GameWorldEvents.EXPLOSION_SOUND).updateExplosionSound(explosion, sound);
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch explosion event", e);
			}
		}
		return sound;
	}

	@SubscribeEvent
	public void onExplosionDetonate(ExplosionEvent.Detonate event) {
		IGamePhase game = gameLookup.getGamePhaseAt(event.getLevel(), BlockPos.containing(event.getExplosion().center()));
		if (game != null) {
			try {
				game.invoker(GameWorldEvents.EXPLOSION_DETONATE).onExplosionDetonate(event.getExplosion(), event.getAffectedBlocks(), event.getAffectedEntities());
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch explosion event", e);
			}
		}
	}

	@SubscribeEvent
	public void onExplosionKnockback(ExplosionKnockbackEvent event) {
		Entity entity = event.getAffectedEntity();
		IGamePhase game = gameLookup.getGamePhaseFor(entity);
		if (game != null) {
			try {
				event.setKnockbackVelocity(game.invoker(GameLivingEntityEvents.MODIFY_EXPLOSION_KNOCKBACK).getKnockback(entity, event.getExplosion(), event.getKnockbackVelocity(), event.getKnockbackVelocity()));
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch explosion event", e);
			}
		}
	}

	@SubscribeEvent
	public void onTreeGrow(BlockGrowFeatureEvent event) {
		IGamePhase game = gameLookup.getGamePhaseAt((Level) event.getLevel(), event.getPos());
		if (game != null) {
			try {
				TriState result = game.invoker(GameWorldEvents.SAPLING_GROW).onSaplingGrow(game.level(), event.getPos());
				if (result.isFalse()) {
					event.setCanceled(true);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch tree grow event", e);
			}
		}
	}

	@SubscribeEvent
	public void onCropGrow(CropGrowEvent.Pre event) {
		IGamePhase game = gameLookup.getGamePhaseAt((Level) event.getLevel(), event.getPos());
		if (game != null) {
			try {
				TriState result = game.invoker(GameWorldEvents.CROP_GROW).onCropGrow(game.level(), event.getPos());
				if (result.isFalse()) {
					event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
				} else if(result.isTrue()) {
					event.setResult(CropGrowEvent.Pre.Result.GROW);
				} else {
					event.setResult(CropGrowEvent.Pre.Result.DEFAULT);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch crop grow event", e);
			}
		}
	}

	@SubscribeEvent
	public void onEntityMount(EntityMountEvent event) {
		Entity entityMounting = event.getEntityMounting();
		Entity entityBeingMounted = event.getEntityBeingMounted();

		IGamePhase game = gameLookup.getGamePhaseFor(entityBeingMounted);
		if (game != null) {
			try {
				TriState result = game.invoker(GameEntityEvents.MOUNTED).onEntityMounted(entityMounting, entityBeingMounted);
				if (result.isFalse()) {
					event.setCanceled(true);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch entity mount event", e);
			}
		}
	}

	@SubscribeEvent
	public void onItemPickup(ItemEntityPickupEvent.Pre event) {
		ItemEntity itemEntity = event.getItemEntity();
		IGamePhase game = gameLookup.getGamePhaseFor(itemEntity);
		if (game == null) {
			return;
		}
		if (event.getPlayer() instanceof ServerPlayer serverPlayer && game.allPlayers().contains(serverPlayer)) {
			try {
				PickUpResult result = game.invoker(GamePlayerEvents.PICK_UP_ITEM).onPickUpItem(serverPlayer, itemEntity);
				switch (result) {
					case DISCARD -> {
						event.setCanPickup(TriState.TRUE);
						event.getItemEntity().getItem().setCount(0);
					}
					case CANCEL -> event.setCanPickup(TriState.FALSE);
				}
			} catch (Exception e) {
				LoveTropics.LOGGER.warn("Failed to dispatch item pickup event", e);
			}
		}
	}

	@SubscribeEvent
	public void onBlockDrops(BlockDropsEvent event) {
		if (event.getBreaker() instanceof ServerPlayer player) {
			IGamePhase game = gameLookup.getGamePhaseFor(player);
			if (game == null) {
				return;
			}
			game.invoker(GameWorldEvents.BLOCK_DROPS).updateBlockDrops(player, event.getPos(), event.getState(), event.getBlockEntity(), event.getTool(), event.getDrops());
		}
	}

	public ContainerListener createInventoryListener(ServerPlayer player) {
		return new InventoryListener(player, gameLookup);
	}

	// Note: equality is important to avoid adding duplicates
	private record InventoryListener(ServerPlayer player, IGameLookup gameLookup) implements ContainerListener {
		@Override
		public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
			IGamePhase game = gameLookup.getGamePhaseFor(player);
			if (game != null) {
				game.invoker(GamePlayerEvents.INVENTORY_CHANGED).onInventoryChanged(player, containerToSend, dataSlotIndex, stack);
			}
		}

		@Override
		public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {
		}
	}

	@SubscribeEvent
	public void onEntityAddedToLevel(EntityJoinLevelEvent event) {
		IGamePhase gamePhase = gameLookup.getGamePhaseInDimension(event.getLevel());
		if (gamePhase != null) {
			gamePhase.invoker(GameWorldEvents.ENTITY_ADDED).onEntityAdded(event.getEntity());
		}
	}

	@SubscribeEvent
	public void onEntityRemovedFromLevel(EntityLeaveLevelEvent event) {
		IGamePhase gamePhase = gameLookup.getGamePhaseInDimension(event.getLevel());
		if (gamePhase != null) {
			gamePhase.invoker(GameWorldEvents.ENTITY_REMOVED).onEntityRemoved(event.getEntity());
		}
	}

	@SubscribeEvent
	public void onEntityRemovedFromLevel(ProjectileImpactEvent event) {
		IGamePhase gamePhase = gameLookup.getGamePhaseInDimension(event.getProjectile().level());
		if (gamePhase != null) {
			gamePhase.invoker(GameWorldEvents.PROJECTILE_IMPACT).onProjectileImpact(event.getProjectile(), event.getRayTraceResult());
		}
	}

	@SubscribeEvent
	public void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
		if (event.getSpawnType() == EntitySpawnReason.CHUNK_GENERATION) {
			// Not main thread, not safe!
			return;
		}
		IGamePhase game = gameLookup.getGamePhaseAt(event.getLevel().getLevel(), event.getPos());
		if (game != null) {
			switch (game.invoker(GameWorldEvents.SPAWN_PLACEMENT_CHECK).canSpawn(event.getPos(), event.getSpawnType(), event.getEntityType())) {
				case TRUE -> event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
				case FALSE -> event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
			}
		}
	}

	private record LoadedChunk(
			ResourceKey<Level> dimension,
			ChunkPos pos
	) {
	}
}
