package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.state.BeaconState;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class WorldBorderGameBehavior implements IGameBehavior {
	public static final MapCodec<WorldBorderGameBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("world_border_center").forGetter(c -> c.worldBorderCenterKey),
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(c -> c.channel),
			ProgressionPeriod.CODEC.fieldOf("period").forGetter(c -> c.period),
			Codec.INT.fieldOf("particle_height").forGetter(c -> c.particleHeight),
			Codec.INT.fieldOf("damage_rate_delay").forGetter(c -> c.damageRateDelay),
			Codec.INT.fieldOf("damage_amount").forGetter(c -> c.damageAmount),
			ExtraCodecs.compactListCodec(ParticleType.CODEC).fieldOf("border_particles").forGetter(c -> c.borderParticles)
	).apply(i, WorldBorderGameBehavior::new));

	private final String worldBorderCenterKey;
	private final ProgressChannel channel;
	private final ProgressionPeriod period;
	private final int particleHeight;
	private final int damageRateDelay;
	private final int damageAmount;
	private final List<ParticleType> borderParticles;

	private BlockPos worldBorderCenter = BlockPos.ZERO;

	private ProgressHolder phases;

	private boolean addedBeacon;

	public WorldBorderGameBehavior(final String worldBorderCenterKey, final ProgressChannel channel, final ProgressionPeriod period,
								   final int particleHeight, final int damageRateDelay, final int damageAmount, final List<ParticleType> borderParticles) {
		this.worldBorderCenterKey = worldBorderCenterKey;
		this.channel = channel;
		this.period = period;
		this.particleHeight = particleHeight;
		this.damageRateDelay = damageRateDelay;
		this.damageAmount = damageAmount;
		this.borderParticles = borderParticles;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		phases = channel.getOrThrow(game);

		List<BlockBox> regions = new ArrayList<>(game.mapRegions().get(worldBorderCenterKey));

		if (!regions.isEmpty()) {
			BlockBox centerRegion = regions.get(game.level().getRandom().nextInt(regions.size()));
			worldBorderCenter = centerRegion.centerBlock();
		} else {
			worldBorderCenter = BlockPos.ZERO;
		}

		events.listen(GamePhaseEvents.TICK, () -> tickWorldBorder(game));

		events.listen(GamePlayerEvents.REMOVE, player -> GameClientState.removeFromPlayer(GameClientStateTypes.BEACON.get(), player));
	}

	// TODO: Clean up this mess
	private void tickWorldBorder(final IGamePhase game) {
		float phaseProgress = phases.progressIn(period);
		if (phaseProgress <= 0.0f) {
			return;
		}

		if (!addedBeacon) {
			BeaconState beacons = game.state().get(BeaconState.KEY);
			beacons.add(game.level().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, worldBorderCenter));
			beacons.sendTo(game.allPlayers());
			addedBeacon = true;
		}

		boolean isCollapsing = phaseProgress >= 1.0f;
		float borderPercent = Math.max(1.0f - phaseProgress, 0.01f);

		float maxRadius = 210;
		float currentRadius = maxRadius * borderPercent;

		for (ParticleType particle : borderParticles) {
			if (game.ticks() % particle.rate == 0) {
				tickParticles(particle, currentRadius, game.level());
			}
		}

		if (game.ticks() % damageRateDelay == 0) {
			tickPlayerDamage(game, isCollapsing, currentRadius);
		}

		//instance.getParticipants()
		//this.actionAllParticipants(instance, (p) -> p.addPotionEffect(new EffectInstance(Effects.SLOW_FALLING, 10 * 20)));

		//world.addParticle(borderParticle, );
	}

	private void tickParticles(ParticleType particle, float currentRadius, ServerLevel world) {
		float amountPerCircle = particle.density * currentRadius;
		float stepAmount = 360F / amountPerCircle;

		int yMin = -10;
		int yStepAmount = 5;

		for (float step = 0; step <= 360; step += stepAmount) {
			for (int yStep = yMin; yStep < particleHeight; yStep += yStepAmount) {
				if (world.random.nextInt(particle.chance/*yMax - yMin*/) == 0) {
					float xVec = (float) -Math.sin(Math.toRadians(step)) * currentRadius;
					float zVec = (float) Math.cos(Math.toRadians(step)) * currentRadius;
					//world.addParticle(borderParticle, worldBorderCenter.getX() + xVec, worldBorderCenter.getY() + yStep, worldBorderCenter.getZ() + zVec, 0, 0, 0);
					//IParticleData data = ForgeRegistries.PARTICLE_TYPES.getValue(new ResourceLocation("heart"));
					world.sendParticles(particle.particle, worldBorderCenter.getX() + xVec, worldBorderCenter.getY() + yStep, worldBorderCenter
							.getZ() + zVec, 1, 0, 0, 0, 1D);
				}
			}
		}

		//serverWorld.spawnParticle(borderParticle, worldBorderCenter.getX(), worldBorderCenter.getY(), worldBorderCenter.getZ(), 1, 0, 0, 0, 1D);
	}

	private void tickPlayerDamage(IGamePhase game, boolean isCollapsing, float currentRadius) {
		for (ServerPlayer player : game.participants()) {
			//ignore Y val, only do X Z dist compare
			double distanceSq = player.distanceToSqr(worldBorderCenter.getX(), player.getY(), worldBorderCenter.getZ());
			if (isCollapsing || !(currentRadius < 0.0 || distanceSq < currentRadius * currentRadius)) {
				player.hurtServer(game.level(), player.damageSources().explosion(null, null), damageAmount);
				player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, SharedConstants.TICKS_PER_SECOND * 2, 0));
			}
		}
	}

	public record ParticleType(ParticleOptions particle, int rate, int chance, float density) {
		public static final Codec<ParticleType> CODEC = RecordCodecBuilder.create(i -> i.group(
				ParticleTypes.CODEC.fieldOf("particle").forGetter(ParticleType::particle),
				Codec.INT.fieldOf("rate").forGetter(ParticleType::rate),
				Codec.INT.fieldOf("chance").forGetter(ParticleType::chance),
				Codec.FLOAT.fieldOf("density").forGetter(ParticleType::density)
		).apply(i, ParticleType::new));
	}
}
