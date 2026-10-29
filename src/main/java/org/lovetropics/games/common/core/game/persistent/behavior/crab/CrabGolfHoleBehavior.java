package org.lovetropics.games.common.core.game.persistent.behavior.crab;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviorType;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;
import org.lovetropics.games.common.core.game.region.LevelBlockPos;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CrabGolfHoleBehavior implements PersistentGameBehavior {

	private static final UUID SEARGE_UUID = UUID.fromString("696a82ce-41f4-4b51-aa31-b8709b8686f0");

	public static final MapCodec<CrabGolfHoleBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.fieldOf("hole").forGetter(b -> b.hole),
			RegionQuery.CODEC.fieldOf("main_region").forGetter(b -> b.mainRegion),
			RegionQuery.CODEC.fieldOf("start_region").forGetter(b -> b.startRegion),
			RegionQuery.CODEC.fieldOf("hole_region").forGetter(b -> b.holeRegion),
			RegionQuery.CODEC.fieldOf("button_region").forGetter(b -> b.buttonRegion),
			RegionQuery.CODEC.fieldOf("winner_region").forGetter(b -> b.winnerRegion),
			Codec.FLOAT.fieldOf("winner_rotation").forGetter(b -> b.winnerRotation),
			TeleportTarget.CODEC.listOf().optionalFieldOf("teleporters").forGetter(b -> Optional.of(b.teleporters))
	).apply(instance, CrabGolfHoleBehavior::new));

	private static final EntitySpawnRequest SPAWN_REQUEST = new EntitySpawnRequest(EntitySpawnReason.COMMAND, false);


	private final int hole;
	private final RegionQuery mainRegion;
	private final RegionQuery startRegion;
	private final RegionQuery holeRegion;
	private final RegionQuery buttonRegion;
	private final RegionQuery winnerRegion;
	private final float winnerRotation;
	private final List<TeleportTarget> teleporters;

	private final Map<ServerPlayer, LivingEntity> currentlyPlaying = new HashMap<>();
	private final Map<ServerPlayer, Integer> ticksLeft = new HashMap<>();
	private final Map<ServerPlayer, Integer> hits = new HashMap<>();

	public CrabGolfHoleBehavior(int hole, RegionQuery mainRegion, RegionQuery startRegion, RegionQuery holeRegion, RegionQuery buttonRegion, RegionQuery winnerRegion, float winnerRotation, Optional<List<TeleportTarget>> teleporters) {
		this.hole = hole;
		this.mainRegion = mainRegion;
		this.startRegion = startRegion;
		this.holeRegion = holeRegion;
		this.buttonRegion = buttonRegion;
		this.winnerRegion = winnerRegion;
		this.winnerRotation = winnerRotation;
		this.teleporters = teleporters.orElse(List.of());
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, initiator -> {
			CrabGolfWinBehavior.GolfData data = CrabGolfWinBehavior.GolfData.get(game.level());
			int highScore = data.getHighScore(hole);
			UUID uuid = data.getHighScoreUUIDFor(hole);

			updateWinnerRegion(game, highScore, uuid);
		});

		events.listen(CrabGolfEvents.HIGH_SCORE, (hole, player, score) -> {
			if (hole == this.hole) {
				updateWinnerRegion(game, score, player.getUUID());
			}
		});

		events.listen(GamePhaseEvents.STOP, initiator -> {
			// TODO: copy out data
			currentlyPlaying.values().forEach(Entity::discard);
			winnerRegion.getEntitiesOfClass(game, ArmorStand.class)
					.forEach(Entity::discard);
		});

		events.listen(CrabGolfEvents.QUERY_PLAYING, currentlyPlaying::containsKey);

		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, hit) -> {
			if (level.getBlockState(pos).is(BlockTags.BUTTONS) && buttonRegion.contains(game, level, pos)) {
				// Already playing?
				if (!game.invoker(CrabGolfEvents.QUERY_PLAYING).isPlaying(player) && currentlyPlaying.isEmpty()) {
					game.invoker(CrabGolfEvents.START_GAME).onStart(hole, player);
				}
			}
			return InteractionResult.PASS;
		});

		events.listen(GamePlayerEvents.ATTACK, (player, target) -> {
			LivingEntity crab = currentlyPlaying.get(player);
			if (target == crab) {
				hits.computeIfPresent(player, (k, v) -> ++v);
			}

			return TriState.DEFAULT;
		});

		events.listen(CrabGolfEvents.START_GAME, (hole, player) -> {
			if (hole == this.hole) {
				LevelBlockPos startPos = startRegion.pickRandomPos(game, player.getRandom());
				if (startPos == null) {
					return;
				}

				Vec3 center = Vec3.atBottomCenterOf(startPos.pos());

				LivingEntity entity = game.invoker(CrabGolfEvents.SUMMON_CRAB).summon(player.level(), player, center);
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
			if (currentlyPlaying.isEmpty()) {
				return;
			}

			for (Map.Entry<ServerPlayer, LivingEntity> e : new HashSet<>(currentlyPlaying.entrySet())) {
				LivingEntity entity = e.getValue();

				ServerPlayer player = e.getKey();
				if (!game.level().players().contains(player)) {
					entity.setHealth(-100);
					entity.kill(game.level());
					currentlyPlaying.remove(player);
					continue;
				}

				Vec3 velocity = entity.getDeltaMovement();
				if (Math.abs(velocity.x) < 0.01 && Math.abs(velocity.z) < 0.01) {
					if (holeRegion.intersects(game, entity.level(), entity.getBoundingBox().contract(0.2, 0, 0.2))) {
						// win!!

						int score = hits.get(player);
						player.sendSystemMessage(Component.literal("Score: " + score), true);
						entity.setHealth(-100);
						entity.kill(game.level());
						currentlyPlaying.remove(player);
						ticksLeft.remove(player);
						hits.remove(player);

						game.invoker(CrabGolfEvents.WIN_GAME).onWin(hole, player, score);
						break;
					}
				}

				if (!mainRegion.intersects(game, entity.level(), entity.getBoundingBox())) {
					entity.snapTo(Vec3.atBottomCenterOf(startRegion.pickRandomPos(game, entity.getRandom()).pos()));
					entity.setDeltaMovement(0, 0, 0);
				}

				int time = ticksLeft.computeIfPresent(player, (k, v) -> --v);
				player.sendSystemMessage(Component.literal("Seconds left: " + (time / 20) + " | Hits: " + hits.get(player)), true);

				if (time == 0) {
					// Ran out of time?

					entity.setHealth(-100);
					entity.kill(game.level());
					currentlyPlaying.remove(player);
					ticksLeft.remove(player);

					game.invoker(CrabGolfEvents.WIN_GAME).onWin(hole, player, -1);
					break;
				}

				for (TeleportTarget teleporter : teleporters) {
					teleporter.tryTeleport(game, entity);
				}
			}
		});
	}

	private void updateWinnerRegion(PersistentGame game, int score, @Nullable UUID player) {
		resolveProfile(game, player).thenAcceptAsync(
				profile -> updateWinnerRegion(game, score, profile),
				game.server()
		);
	}

	private void updateWinnerRegion(PersistentGame game, int score, ResolvableProfile profile) {
		Region winnerRegion = this.winnerRegion.nullableOne(game);
		if (winnerRegion == null) {
			return;
		}

		ServerLevel level = winnerRegion.level();
		this.winnerRegion.loadAndWaitForEntities(game);

		// Dummy players inherit from ArmorStand right now
		this.winnerRegion.getEntitiesOfClass(game, ArmorStand.class)
				.forEach(Entity::discard);

		CompoundTag nbt = new CompoundTag();
		nbt.putString("id", "dummyplayers:dummy_player");
		nbt.putInt("DisabledSlots", 4144959);
		nbt.putBoolean("Invulnerable", true);

		nbt.store("profile", ResolvableProfile.CODEC, profile);

		if (score > 0) {
			Component prefix = Component.translatable("lt.golf.best_score").withStyle(ChatFormatting.GREEN);
			nbt.store("name_prefix", ComponentSerialization.CODEC, prefix);

			Component suffix = Component.literal(" - " + score).withStyle(ChatFormatting.GREEN);
			nbt.store("name_suffix", ComponentSerialization.CODEC, suffix);
		}

		Vec3 pos = Vec3.atBottomCenterOf(winnerRegion.centerBlock());
		LivingEntity entity = (LivingEntity) EntityType.loadEntityRecursive(nbt, level, SPAWN_REQUEST, (e) -> {
			e.snapTo(pos.x, pos.y, pos.z, winnerRotation, e.getXRot());
			return e;
		});
		level.tryAddFreshEntityWithPassengers(entity);
	}

	private CompletableFuture<ResolvableProfile> resolveProfile(PersistentGame game, @Nullable UUID player) {
		Services services = game.server().services();
		return CompletableFuture.supplyAsync(
				() -> {
					UUID playerId = player == null ? SEARGE_UUID : player;
					Optional<GameProfile> profile = services.profileResolver().fetchById(playerId);
					return profile.map(ResolvableProfile::createResolved).orElseGet(() -> ResolvableProfile.createUnresolved(playerId));
				},
				Util.nonCriticalIoPool()
		);
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CRAB_GOLF_HOLE;
	}

	public static class TeleportTarget {
		public static final Codec<TeleportTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				RegionQuery.CODEC.fieldOf("in").forGetter(b -> b.in),
				RegionQuery.CODEC.fieldOf("out").forGetter(b -> b.out),
				Codec.BOOL.fieldOf("needs_stop").forGetter(b -> b.needsStop)
		).apply(instance, TeleportTarget::new));
		private final RegionQuery in;
		private final RegionQuery out;
		private final boolean needsStop;

		public TeleportTarget(RegionQuery in, RegionQuery out, boolean needsStop) {
			this.in = in;
			this.out = out;
			this.needsStop = needsStop;
		}

		public void tryTeleport(PersistentGame game, LivingEntity entity) {
			Vec3 velocity = entity.getDeltaMovement();
			if (needsStop && !(Math.abs(velocity.x) < 0.01 && Math.abs(velocity.z) < 0.01)) {
				return;
			}

			if (in.intersects(game, entity.level(), entity.getBoundingBox())) {
				entity.snapTo(Vec3.atBottomCenterOf(out.pickRandomPos(game, entity.getRandom()).pos()));
			}
		}
	}
}
