package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantCoverage;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantPlacement;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantState;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

public final class ScareTrapPlantBehavior implements IGameBehavior {
	public static final MapCodec<ScareTrapPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.fieldOf("trigger_radius").forGetter(c -> c.triggerRadius),
			Codec.DOUBLE.fieldOf("scare_radius").forGetter(c -> c.scareRadius)
	).apply(i, ScareTrapPlantBehavior::new));

	private static final Predicate<Mob> SCARE_PREDICATE = BbMobEntity.PREDICATE;

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
		plots = game.state().getOrThrow(PlotsState.KEY);

		events.listen(BbPlantEvents.ADD, (player, plot, plant) -> {
			plant.state().put(Trap.KEY, new Trap());
		});

		events.listen(BbPlantEvents.PLACE, this::place);
		events.listen(BbPlantEvents.TICK, this::tick);
		events.listen(GamePlayerEvents.USE_BLOCK, this::useBlock);
	}

	private PlantPlacement place(ServerPlayer player, Plot plot, BlockPos pos) {
		return new PlantPlacement()
				.covers(buildPlantCoverage(plot, pos))
				.places((world, coverage) -> {
					placeReadyTrap(plot, pos);
					return true;
				});
	}

	private InteractionResult useBlock(ServerPlayer player, ServerLevel world, BlockPos pos, InteractionHand hand, BlockHitResult traceResult) {
		if (world.getBlockState(pos).is(Blocks.LEVER)) {
			return useLever(player, pos);
		}

		return InteractionResult.PASS;
	}

	private InteractionResult useLever(ServerPlayer player, BlockPos pos) {
		Plot plot = plots.getPlotFor(player);
		if (plot != null && plot.bounds.contains(pos)) {
			Plant plant = plot.plants.getPlantAt(pos);
			if (plant != null && resetTrap(plot, plant)) {
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.FAIL;
	}

	private void tick(PlayerSet players, Plot plot, List<Plant> plants) {
		long ticks = game.ticks();
		if (ticks % 10 != 0) {
			return;
		}

		for (Plant plant : plants) {
			Trap trap = plant.state(Trap.KEY);
			if (trap == null || !trap.ready) {
				continue;
			}

			if (tickTrap(plot, plant)) {
				trap.ready = false;
			}
		}
	}

	private boolean tickTrap(Plot plot, Plant plant) {
		// TODO: mobs should run around in panic after being scared

		AABB bounds = plant.coverage().asBounds();
		AABB triggerBounds = bounds.inflate(triggerRadius);

		ServerLevel world = game.level();

		List<Mob> triggerEntities = world.getEntitiesOfClass(Mob.class, triggerBounds, SCARE_PREDICATE);
		if (triggerEntities.isEmpty()) {
			return false;
		}

		AABB scareBounds = bounds.inflate(scareRadius);
		List<Mob> entities = world.getEntitiesOfClass(Mob.class, scareBounds, SCARE_PREDICATE);
		triggerTrap(plot, plant, entities);

		return true;
	}

	private void triggerTrap(Plot plot, Plant plant, List<Mob> entities) {
		BlockPos origin = plant.coverage().getOrigin();
		Vec3 pushFrom = Vec3.atCenterOf(origin);

		for (Mob entity : entities) {
			scareEntity(origin, pushFrom, entity);
		}

		extendTrap(plot, plant);
	}

	private void scareEntity(BlockPos pos, Vec3 pushFrom, Mob entity) {
		Vec3 entityPos = entity.position();

		// Scaled so that closer values are higher, with a max of 5
		double dist = 2.0 / (0.1 + entityPos.distanceTo(pushFrom));

		// Angle between entity and center of lantern
		double theta = Math.atan2(entityPos.z - pushFrom.z, entityPos.x - pushFrom.x);

		// zoooooom
		entity.push(dist * Math.cos(theta), 0.25, dist * Math.sin(theta));

		// Prevent mobs from flying to the moon due to too much motion
		Vec3 motion = entity.getDeltaMovement();
		entity.setDeltaMovement(Math.min(motion.x, 5), Math.min(motion.y, 0.25), Math.min(motion.z, 5));

		if (entity instanceof BbMobEntity) {
			((BbMobEntity) entity).getMobBrain().addScarySource(pos);
		}

		// Spawn critical hit particles around the entity
		game.allPlayers().sendPacket(new ClientboundAnimatePacket(entity, 4));
	}

	private void extendTrap(Plot plot, Plant plant) {
		Trap trap = plant.state(Trap.KEY);
		if (trap == null || !trap.trigger(game)) {
			return;
		}

		BlockPos origin = plant.coverage().getOrigin();

		game.level().playSound(null, origin, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 1.0F);

		clearTrap(plant);
		placeExtendedTrap(plot, origin);
	}

	private boolean resetTrap(Plot plot, Plant plant) {
		Trap trap = plant.state(Trap.KEY);
		if (trap == null || !trap.tryReset(game)) {
			return false;
		}

		BlockPos origin = plant.coverage().getOrigin();

		game.level().playSound(null, origin, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.0F, 1.0F);

		clearTrap(plant);
		placeReadyTrap(plot, origin);

		return true;
	}

	private void clearTrap(Plant plant) {
		ServerLevel world = game.level();
		for (BlockPos pos : plant.coverage()) {
			world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
		}
	}

	private void placeExtendedTrap(Plot plot, BlockPos pos) {
		ServerLevel world = game.level();

		world.setBlockAndUpdate(pos, Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonHeadBlock.FACING, Direction.UP));
		world.setBlockAndUpdate(pos.above(), Blocks.JACK_O_LANTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, plot.forward));

		BlockState lever = Blocks.LEVER.defaultBlockState()
				.setValue(LeverBlock.FACING, plot.forward.getOpposite())
				.setValue(LeverBlock.FACE, AttachFace.WALL);
		world.setBlockAndUpdate(pos.above().relative(plot.forward.getOpposite()), lever);
	}

	private void placeReadyTrap(Plot plot, BlockPos pos) {
		ServerLevel world = game.level();
		world.setBlockAndUpdate(pos, Blocks.JACK_O_LANTERN.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, plot.forward));
	}

	private PlantCoverage buildPlantCoverage(Plot plot, BlockPos pos) {
		// TODO: duplication
		return new PlantCoverage.Builder()
				.add(pos).add(pos.above())
				.add(pos.above().relative(plot.forward.getOpposite()))
				.build();
	}

	static final class Trap {
		private static final long TRIGGER_FROZEN_TICKS = 5 * 20;

		static final PlantState.Key<Trap> KEY = PlantState.Key.create();

		boolean ready = true;
		long frozenExpiry;

		boolean trigger(IGamePhase game) {
			if (ready) {
				frozenExpiry = game.ticks() + TRIGGER_FROZEN_TICKS;
				ready = false;
				return true;
			} else {
				return false;
			}
		}

		boolean tryReset(IGamePhase game) {
			if (!ready && game.ticks() > frozenExpiry) {
				ready = true;
				return true;
			} else {
				return false;
			}
		}
	}
}
