package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.tutorial.TutorialState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbMobSpawnState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.explosion.FilteredExplosion;
import com.lovetropics.minigames.common.content.biodiversity_blitz.explosion.PlantAffectingExplosion;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.dimension.DimensionUtils;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

// TODO: needs to be split up & data-driven more!

public final class BbBehavior implements IGameBehavior {
	public static final MapCodec<BbBehavior> CODEC = MapCodec.unit(BbBehavior::new);

	private IGamePhase game;
	private TeamState teams;
	private PlotsState plots;
	private TutorialState tutorial;

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;
		teams = game.instanceState().getOrThrow(TeamState.KEY);
		plots = game.state().getOrThrow(PlotsState.KEY);
		tutorial = game.state().getOrThrow(TutorialState.KEY);

		events.listen(GamePlayerEvents.SPAWN, this::setupPlayerAsRole);
		events.listen(BbEvents.ASSIGN_PLOT, this::onAssignPlot);
		events.listen(GamePhaseEvents.TICK, () -> tick(game));
		events.listen(GamePlayerEvents.DEATH, this::onPlayerDeath);
		events.listen(GameWorldEvents.EXPLOSION_DETONATE, this::onExplosion);
		// Don't grow any trees- we handle that ourselves
		events.listen(GameWorldEvents.SAPLING_GROW, (w, p) -> TriState.FALSE);
		events.listen(GamePlayerEvents.ATTACK, this::onAttack);
		// Custom mob drops
		events.listen(GameLivingEntityEvents.MOB_DROP, (e, d, r) -> {
			r.removeIf(i -> !i.getItem().is(BiodiversityBlitz.OSA_POINT.asItem()));

			r.add(new ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(), new ItemStack(BiodiversityBlitz.OSA_POINT.get(), 1)));

			return TriState.DEFAULT;
		});
		events.listen(GameLivingEntityEvents.FARMLAND_TRAMPLE, this::onTrampleFarmland);
		events.listen(GameEntityEvents.MOUNTED, (mounting, beingMounted) -> {
			if (mounting instanceof ServerPlayer) {
				return TriState.DEFAULT;
			} else {
				return TriState.FALSE;
			}
		});
		events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> {
			Plot plot = plots.getPlotFor(player);
			if (plot == null) {
				return TriState.DEFAULT;
			}

			if (!plot.walls.getBounds().contains(player.position())) {
				return TriState.FALSE;
			}

			// Don't damage players from sweet berry bushes or wither roses
			// TODO: reduce slowdown from bush
			if (damageSource.is(DamageTypes.SWEET_BERRY_BUSH) || damageSource.is(DamageTypes.WITHER)) {
				return TriState.FALSE;
			}

			return TriState.DEFAULT;
		});

		events.listen(GamePlayerEvents.PLACE_BLOCK, this::onPlaceBlock);
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> TriState.FALSE);

		events.listen(GamePlayerEvents.USE_BLOCK, this::onUseBlock);
	}

	private InteractionResult onUseBlock(ServerPlayer player, ServerLevel world, BlockPos blockPos, InteractionHand hand, BlockHitResult blockRayTraceResult) {
		if (!tutorial.isTutorialFinished()) {
			return InteractionResult.FAIL;
		}

		Plot plot = plots.getPlotFor(player);
		BlockPos pos = blockRayTraceResult.getBlockPos();

		if (plot != null && plot.bounds.contains(pos)) {
			return onUseBlockInPlot(player, world, blockPos, hand, plot, pos);
		} else {
			return InteractionResult.CONSUME;
		}
	}

	private InteractionResult onUseBlockInPlot(ServerPlayer player, ServerLevel world, BlockPos blockPos, InteractionHand hand, Plot plot, BlockPos pos) {
		BlockState state = world.getBlockState(pos);

		// TODO: can we make it not hardcoded?
		if (plot.isFloorAt(pos) && player.getItemInHand(hand).is(ItemTags.HOES)) {
			// If there is no plant above we can change to grass safely
			if (state.is(Blocks.FARMLAND) && !plot.plants.hasPlantAt(pos.above())) {
				world.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
				world.playSound(null, blockPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
				player.getCooldowns().addCooldown(player.getItemInHand(hand), 3);
				return InteractionResult.SUCCESS;
			} else if (state.is(Blocks.DIRT_PATH)) {
				return InteractionResult.FAIL;
			}
		}

		if (state.is(Blocks.DECORATED_POT)) {
			return InteractionResult.FAIL;
		}

		return InteractionResult.PASS;
	}

	private TriState onTrampleFarmland(Entity entity, BlockPos pos, BlockState state) {
		if (!tutorial.isTutorialFinished()) {
			return TriState.FALSE;
		}

		Plot plot = plots.getPlotFor(entity);
		if (plot != null && plot.isFloorAt(pos)) {
			if (!plot.plants.hasPlantAt(pos.above())) {
				return TriState.DEFAULT;
			}
		}

		return TriState.FALSE;
	}

	private void setupPlayerAsRole(UUID playerId, SpawnBuilder spawn, @Nullable PlayerRole role) {
		if (role == PlayerRole.SPECTATOR) {
			spawnSpectator(spawn);
		}
	}

	private void spawnSpectator(SpawnBuilder spawn) {
		ServerLevel level = game.level();
		RandomSource random = level.getRandom();
		Plot plot = plots.getRandomPlot(random);
		if (plot != null) {
			spawn.teleportTo(level, plot.plantBounds.sample(random).above(5), plot.forward);
		}

		spawn.setGameMode(GameType.SPECTATOR);
	}

	private void onAssignPlot(ServerPlayer player, Plot plot) {
		GameClientState.sendToPlayer(new ClientBbMobSpawnState(plot.mobSpawns), player);
		teleportToRegion(player, plot.spawn, plot.forward);
	}

	private void onExplosion(Explosion explosion, List<BlockPos> affectedBlocks, List<Entity> affectedEntities) {
		affectedEntities.removeIf(e -> e instanceof Player);

		// Remove from filtered explosions
		if (explosion instanceof FilteredExplosion filteredExplosion) {
			affectedEntities.removeIf(filteredExplosion.remove);
		}

		if (explosion instanceof PlantAffectingExplosion plantAffectingExplosion) {
			plantAffectingExplosion.affectPlants(affectedBlocks);
		}

		// Blocks should not explode
		affectedBlocks.clear();
	}

	private TriState onAttack(ServerPlayer player, Entity target) {
		if (!tutorial.isTutorialFinished()) {
			return TriState.FALSE;
		}

		if (BbMobEntity.matches(target)) {
			Plot plot = plots.getPlotAt(target.blockPosition());
			if (plot != null && plot.walls.containsEntity(player)) {
				return TriState.DEFAULT;
			}
		}
		return TriState.FALSE;
	}

	private TriState onPlaceBlock(ServerPlayer player, BlockPos pos, BlockState placed, BlockState placedOn, ItemStack placedItemStack) {
		if (!tutorial.isTutorialFinished()) {
			return TriState.FALSE;
		}

		Plot plot = plots.getPlotFor(player);
		if (plot != null && plot.bounds.contains(pos)) {
			// Don't let players place plants inside mob spawns
			if (plot.mobSpawns.contains(pos)) {
				sendActionRejection(player, BiodiversityBlitzTexts.PLANT_CANNOT_FIT);
				return TriState.FALSE;
			}

			return onPlaceBlockInOwnPlot(player, pos, placed, plot);
		} else {
			sendActionRejection(player, BiodiversityBlitzTexts.NOT_YOUR_PLOT);
			return TriState.FALSE;
		}
	}

	private TriState onPlaceBlockInOwnPlot(ServerPlayer player, BlockPos pos, BlockState placed, Plot plot) {
		if (placed.is(Blocks.FARMLAND)) {
			player.level().setBlockAndUpdate(pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
			return TriState.DEFAULT;
		}
		// TODO: Data-drive
		if (placed.is(Blocks.ANVIL)) {
			return TriState.DEFAULT;
		}

		if (plot.canPlantAt(pos)) {
			sendActionRejection(player, BiodiversityBlitzTexts.CAN_ONLY_PLACE_PLANTS);
		}

		return TriState.FALSE;
	}

	private void sendActionRejection(ServerPlayer player, Component message) {
		player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	private TriState onPlayerDeath(ServerPlayer player, DamageSource damageSource) {
		Plot plot = plots.getPlotFor(player);
		if (plot == null) {
			return TriState.DEFAULT;
		}

		teleportToRegion(player, plot.spawn, plot.forward);
		player.setHealth(20.0F);
		if (player.getFoodData().getFoodLevel() < 10) {
			player.getFoodData().eat(2, 0.8f);
		}

		// We need to encapsulate the currency behavior's death event.
		// Using the standard event system means it'll never be called due to us needing to fail the event, so we duplicate it.
		game.invoker(BbEvents.BB_DEATH).onDeath(player, damageSource);

		player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.18F, 1.0F);
		player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 255, 80));

		player.connection.send(new ClientboundSetTitlesAnimationPacket(40, 20, 0));
		player.connection.send(new ClientboundSetTitleTextPacket(BiodiversityBlitzTexts.DEATH_TITLE.copy().withStyle(ChatFormatting.RED)));

		return TriState.FALSE;
	}

	private void tick(IGamePhase game) {
		for (Plot plot : plots) {
			PlayerSet players = teams.getParticipantsForTeam(game, plot.team);
			game.invoker(BbEvents.TICK_PLOT).onTickPlot(plot, players);
		}
	}

	private void teleportToRegion(ServerPlayer player, BlockBox region, Direction direction) {
		BlockPos pos = region.sample(player.getRandom());

		player.setYRot(direction.toYRot());
		DimensionUtils.teleportPlayerNoPortal(player, game.dimension(), pos);
	}
}
