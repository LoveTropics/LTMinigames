package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.lovetropics.minigames.common.core.game.persistent.behavior.GameRegionBehavior;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.map.SavedRegions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class CrabGolfHoleBehavior implements PersistentGameBehavior {
	public static final MapCodec<CrabGolfHoleBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.fieldOf("hole").forGetter(b -> b.hole),
			Codec.STRING.fieldOf("main_region").forGetter(b -> b.mainRegionName),
			Codec.STRING.fieldOf("start_region").forGetter(b -> b.startRegionName),
			Codec.STRING.fieldOf("hole_region").forGetter(b -> b.holeRegionName),
			Codec.STRING.fieldOf("button_region").forGetter(b -> b.buttonRegionName),
			TeleportTarget.CODEC.listOf().optionalFieldOf("teleporters").forGetter(b -> Optional.of(b.teleporters))
	).apply(instance, CrabGolfHoleBehavior::new));

	private final int hole;
	private final String mainRegionName;
	private final String startRegionName;
	private final String holeRegionName;
	private final String buttonRegionName;
	private final List<TeleportTarget> teleporters;

	private final List<BlockBox> mainRegions = new ArrayList<>();
	private BlockBox startRegion = null;
	private BlockBox holeRegion = null;
	private BlockBox buttonRegion = null;

	private final Map<ServerPlayer, LivingEntity> currentlyPlaying = new HashMap<>();
	private final Map<ServerPlayer, Integer> ticksLeft = new HashMap<>();
	private final Map<ServerPlayer, Integer> hits = new HashMap<>();

	public CrabGolfHoleBehavior(int hole, String mainRegionName, String startRegionName, String holeRegionName, String buttonRegionName, Optional<List<TeleportTarget>> teleporters) {
		this.hole = hole;
		this.mainRegionName = mainRegionName;
		this.startRegionName = startRegionName;
		this.holeRegionName = holeRegionName;
		this.buttonRegionName = buttonRegionName;
		this.teleporters = teleporters.orElse(List.of());
	}

	public boolean isOk() {
		return startRegion != null && holeRegion != null && buttonRegion != null;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, initiator -> {
			MapRegions regions = SavedRegions.get(game.level()).regions().compile();
			mainRegions.addAll(regions.get(mainRegionName));
			startRegion = regions.getAny(startRegionName);
			holeRegion = regions.getAny(holeRegionName);
			buttonRegion = regions.getAny(buttonRegionName);
		});

		events.listen(CrabGolfEvents.QUERY_PLAYING, this.currentlyPlaying::containsKey);

		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, hit) -> {
			if (isOk() && level.getBlockState(pos).is(BlockTags.BUTTONS) && buttonRegion.contains(pos)) {
				// Already playing?
				if (!game.invoker(CrabGolfEvents.QUERY_PLAYING).isPlaying(player)) {
					game.invoker(CrabGolfEvents.START_GAME).onStart(hole, player);
				}
			}
			return InteractionResult.PASS;
		});

		events.listen(GamePlayerEvents.ATTACK, (player, target) -> {
			LivingEntity crab = this.currentlyPlaying.get(player);
			if (target == crab) {
				this.hits.computeIfPresent(player, (k, v) -> ++v);
			}

			return TriState.DEFAULT;
		});

		events.listen(CrabGolfEvents.START_GAME, (hole, player) -> {
			if (isOk() && hole == this.hole) {
				// spawn crab
				Vec3 rcenter = startRegion.center();
				// move to floor
				Vec3 center = new Vec3(rcenter.x, rcenter.y - 0.5, rcenter.z);

				LivingEntity entity = game.invoker(CrabGolfEvents.SUMMON_CRAB).summon(player.level(), center);
				if (entity == null) {
					return;
				}

				player.setGameMode(GameType.ADVENTURE);
				currentlyPlaying.put(player, entity);
				ticksLeft.put(player, 2400);
				hits.put(player, 0);
			}
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (!isOk()) {
				return;
			}
			if (currentlyPlaying.isEmpty()) {
				return;
			}

			for (Map.Entry<ServerPlayer, LivingEntity> e : new HashSet<>(currentlyPlaying.entrySet())) {
				LivingEntity entity = e.getValue();

				ServerPlayer player = e.getKey();
				if (!game.level().players().contains(player)) {
					entity.kill(game.level());
					currentlyPlaying.remove(player);
					continue;
				}

				Vec3 velocity = entity.getDeltaMovement();
				if (Math.abs(velocity.x) < 0.01 && Math.abs(velocity.z) < 0.01) {
					if (holeRegion.asAabb().intersects(entity.getBoundingBox())) {
						// win!!

						int score = this.hits.get(player);
						player.sendSystemMessage(Component.literal("Score: " + score), true);
						entity.kill(game.level());
						currentlyPlaying.remove(player);
						ticksLeft.remove(player);
						hits.remove(player);

						game.invoker(CrabGolfEvents.WIN_GAME).onWin(hole, player, score);
						break;
					}
				}

				boolean inRegion = false;
				for (BlockBox r : mainRegions) {
					if (r.asAabb().intersects(entity.getBoundingBox())) {
						inRegion = true;
						break;
					}
				}

				if (!inRegion) {
					Vec3 rcenter = startRegion.center();
					// move to floor
					Vec3 center = new Vec3(rcenter.x, rcenter.y - 0.5, rcenter.z);
					entity.snapTo(center);
				}

				int time = ticksLeft.computeIfPresent(player, (k, v) -> --v);
				player.sendSystemMessage(Component.literal("Seconds left: " + (time / 20) + " | Hits: " + this.hits.get(player)), true);

				if (time == 0) {
					// Ran out of time?

					entity.kill(game.level());
					currentlyPlaying.remove(player);
					ticksLeft.remove(player);

					game.invoker(CrabGolfEvents.WIN_GAME).onWin(hole, player, -1);
				}
			}
		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CRAB_GOLF_HOLE;
	}

	public static class TeleportTarget {
		public static final Codec<TeleportTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("in").forGetter(b -> b.in),
				Codec.STRING.fieldOf("out").forGetter(b -> b.out),
				Codec.BOOL.fieldOf("needs_stop").forGetter(b -> b.needsStop)
		).apply(instance, TeleportTarget::new));
		private final String in;
		private final String out;
		private final boolean needsStop;

		public TeleportTarget(String in, String out, boolean needsStop) {
			this.in = in;
			this.out = out;
			this.needsStop = needsStop;
		}

		public void initialize() {

		}
	}
}
