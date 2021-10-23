package com.lovetropics.minigames.common.content.mangroves_and_pianguas.behavior.plant;

import com.lovetropics.minigames.common.content.mangroves_and_pianguas.behavior.event.MpPlantEvents;
import com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.Plot;
import com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.PlotsState;
import com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.plant.Plant;
import com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.plant.PlantCoverage;
import com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.plant.state.PlantState;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.StoneButtonBlock;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.state.properties.AttachFace;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.function.Predicate;

public final class ScareTrapPlantBehavior implements IGameBehavior {
	public static final Codec<ScareTrapPlantBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.DOUBLE.fieldOf("trigger_radius").forGetter(c -> c.triggerRadius),
			Codec.DOUBLE.fieldOf("scare_radius").forGetter(c -> c.scareRadius)
	).apply(instance, ScareTrapPlantBehavior::new));

	private static final Predicate<MobEntity> SCARE_PREDICATE = entity -> !(entity instanceof VillagerEntity);

	private final double triggerRadius;
	private final double scareRadius;

	private IGamePhase game;
	private PlotsState plots;

	public ScareTrapPlantBehavior(double triggerRadius, double scareRadius) {
		this.triggerRadius = triggerRadius;
		this.scareRadius = scareRadius;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;
		this.plots = game.getState().getOrThrow(PlotsState.KEY);

		events.listen(MpPlantEvents.ADD, (player, plot, plant) -> {
			plant.state().put(Trap.KEY, new Trap());
		});

		events.listen(MpPlantEvents.PLACE, this::place);
		events.listen(MpPlantEvents.TICK, this::tick);
		events.listen(GamePlayerEvents.USE_BLOCK, this::useBlock);
	}

	private PlantCoverage place(ServerPlayerEntity player, Plot plot, BlockPos pos) {
		this.placeReadyTrap(plot, pos);
		return this.buildPlantCoverage(plot, pos);
	}

	private ActionResultType useBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, Hand hand, BlockRayTraceResult traceResult) {
		Plot plot = plots.getPlotFor(player);
		if (plot != null && plot.bounds.contains(pos)) {
			Plant plant = plot.plants.getPlantAt(pos);
			if (plant != null && this.resetTrap(plot, plant)) {
				return ActionResultType.SUCCESS;
			}
		}

		return ActionResultType.PASS;
	}

	private void tick(ServerPlayerEntity player, Plot plot, List<Plant> plants) {
		long ticks = game.ticks();
		if (ticks % 5 != 0) return;

		for (Plant plant : plants) {
			Trap trap = plant.state(Trap.KEY);
			if (trap == null || !trap.ready) {
				continue;
			}

			if (this.tickTrap(plot, plant)) {
				trap.ready = false;
			}
		}
	}

	private boolean tickTrap(Plot plot, Plant plant) {
		// TODO: once triggered, mob should run around avoiding going near the jack o lantern (even when no longer panicking)

		AxisAlignedBB bounds = plant.coverage().asBounds();
		AxisAlignedBB triggerBounds = bounds.grow(this.triggerRadius);

		ServerWorld world = game.getWorld();

		List<MobEntity> triggerEntities = world.getEntitiesWithinAABB(MobEntity.class, triggerBounds, SCARE_PREDICATE);
		if (triggerEntities.isEmpty()) {
			return false;
		}

		AxisAlignedBB scareBounds = bounds.grow(this.scareRadius);
		List<MobEntity> entities = world.getEntitiesWithinAABB(MobEntity.class, scareBounds, SCARE_PREDICATE);
		this.triggerTrap(plot, plant, bounds, entities);

		return true;
	}

	private void triggerTrap(Plot plot, Plant plant, AxisAlignedBB bounds, List<MobEntity> entities) {
		for (MobEntity entity : entities) {
			this.scareEntity(bounds.getCenter(), entity);
		}

		this.extendTrap(plot, plant);
	}

	private void scareEntity(Vector3d pushFrom, MobEntity entity) {
		Vector3d entityPos = entity.getPositionVec();

		// Scaled so that closer values are higher, with a max of 5
		double dist = 1.5 / (0.1 + entityPos.distanceTo(pushFrom));

		// Angle between entity and center of lantern
		double theta = Math.atan2(entityPos.z - pushFrom.z, entityPos.x - pushFrom.x);

		// zoooooom
		entity.addVelocity(dist * Math.cos(theta), 0.25, dist * Math.sin(theta));

		// Prevent mobs from flying to the moon due to too much motion
		Vector3d motion = entity.getMotion();
		entity.setMotion(Math.min(motion.x, 5), Math.min(motion.y, 0.25), Math.min(motion.z, 5));

		// Spawn critical hit particles around the entity
		game.getAllPlayers().sendPacket(new SAnimateHandPacket(entity, 4));
	}

	private void extendTrap(Plot plot, Plant plant) {
		Trap trap = plant.state(Trap.KEY);
		if (trap == null || !trap.ready) return;

		trap.ready = false;

		BlockPos origin = plant.coverage().getOrigin();

		game.getWorld().playSound(null, origin, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1.0F, 1.0F);

		this.clearTrap(plant);
		this.placeExtendedTrap(plot, origin);
	}

	private boolean resetTrap(Plot plot, Plant plant) {
		Trap trap = plant.state(Trap.KEY);
		if (trap == null || trap.ready) {
			return false;
		}

		trap.ready = true;

		BlockPos origin = plant.coverage().getOrigin();

		game.getWorld().playSound(null, origin, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 1.0F, 1.0F);

		this.clearTrap(plant);
		this.placeReadyTrap(plot, origin);

		return true;
	}

	private void clearTrap(Plant plant) {
		ServerWorld world = game.getWorld();
		for (BlockPos pos : plant.coverage()) {
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
		}
	}

	private void placeExtendedTrap(Plot plot, BlockPos pos) {
		ServerWorld world = game.getWorld();

		world.setBlockState(pos, Blocks.PISTON_HEAD.getDefaultState().with(PistonHeadBlock.FACING, Direction.UP));
		world.setBlockState(pos.up(), Blocks.JACK_O_LANTERN.getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, plot.forward));

		BlockState button = Blocks.STONE_BUTTON.getDefaultState()
				.with(StoneButtonBlock.HORIZONTAL_FACING, plot.forward)
				.with(StoneButtonBlock.FACE, AttachFace.FLOOR);
		world.setBlockState(pos.offset(plot.forward.getOpposite()), button);
	}

	private void placeReadyTrap(Plot plot, BlockPos pos) {
		ServerWorld world = game.getWorld();
		world.setBlockState(pos, Blocks.JACK_O_LANTERN.getDefaultState().with(BlockStateProperties.HORIZONTAL_FACING, plot.forward));
	}

	private PlantCoverage buildPlantCoverage(Plot plot, BlockPos pos) {
		// TODO: duplication
		return new PlantCoverage.Builder()
				.add(pos).add(pos.up())
				.add(pos.offset(plot.forward.getOpposite()))
				.build();
	}

	static final class Trap {
		static final PlantState.Key<Trap> KEY = PlantState.Key.create();

		boolean ready = true;
	}
}
