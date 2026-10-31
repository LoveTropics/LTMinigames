package org.lovetropics.games.common.core.game.persistent.behavior.crab;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
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
	private static final GameType FORCED_GAME_MODE = GameType.ADVENTURE;

	private final int hole;
	private final RegionQuery mainRegion;
	private final RegionQuery startRegion;
	private final RegionQuery holeRegion;
	private final RegionQuery buttonRegion;
	private final RegionQuery winnerRegion;
	private final float winnerRotation;
	private final List<TeleportTarget> teleporters;

	private final Map<UUID, PlayerHoleState> currentlyPlaying = new HashMap<>();

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
			GolfData data = GolfData.get(game.level());
			GolfData.Score highScore = data.getHighScore(hole);

			if (highScore != null) {
				updateWinnerRegion(game, highScore.score(), highScore.profile().id());
			} else {
				updateWinnerRegion(game, 0, (UUID) null);
			}
		});

		events.listen(CrabGolfEvents.HIGH_SCORE, (hole, player, score) -> {
			if (hole == this.hole) {
				updateWinnerRegion(game, score, player.getUUID());
			}
		});

		events.listen(GamePhaseEvents.STOP, _ -> {
			// TODO: copy out data
			currentlyPlaying.values().forEach(PlayerHoleState::discard);
			winnerRegion.getEntitiesOfClass(game, ArmorStand.class)
					.forEach(Entity::discard);
		});

		events.listen(CrabGolfEvents.QUERY_PLAYING, player -> currentlyPlaying.containsKey(player.getUUID()));

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
			PlayerHoleState state = currentlyPlaying.get(player.getUUID());
			if (state != null && target == state.crab) {
				state.hits++;
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

				GameType gameMode = player.gameMode();
				player.setGameMode(FORCED_GAME_MODE);
				currentlyPlaying.put(player.getUUID(), new PlayerHoleState(gameMode, entity, 2400));
			}
		});

		events.listen(GamePhaseEvents.TICK, () -> tickHoles(game));
	}

	private void tickHoles(PersistentGame game) {
		currentlyPlaying.entrySet().removeIf(entry -> {
			PlayerHoleState state = entry.getValue();
			ServerPlayer player = (ServerPlayer) game.level().getPlayerByUUID(entry.getKey());
			if (player == null) {
				state.stopGame(null);
				return true;
			}
			TickResult result = tickHole(game, state, player);
			return switch (result) {
				case CONTINUE -> false;
				case WIN -> {
					int score = state.hits;
					player.sendSystemMessage(Component.literal("Score: " + score), true);

					game.invoker(CrabGolfEvents.WIN_GAME).onWin(hole, player, score);
					state.stopGame(player);
					yield true;
				}
				case CANCEL -> {
					game.invoker(CrabGolfEvents.WIN_GAME).onWin(hole, player, -1);
					state.stopGame(player);
					yield true;
				}
			};
		});
	}

	private TickResult tickHole(PersistentGame game, PlayerHoleState state, ServerPlayer player) {
		LivingEntity entity = state.crab;

		Vec3 velocity = entity.getDeltaMovement();
		if (Math.abs(velocity.x) < 0.01 && Math.abs(velocity.z) < 0.01) {
			if (holeRegion.intersects(game, entity.level(), entity.getBoundingBox().contract(0.2, 0, 0.2))) {
				return TickResult.WIN;
			}
		}

		if (!mainRegion.intersects(game, entity.level(), entity.getBoundingBox())) {
			entity.snapTo(Vec3.atBottomCenterOf(startRegion.pickRandomPos(game, entity.getRandom()).pos()));
			entity.setDeltaMovement(0, 0, 0);
		}

		int time = state.ticksLeft--;
		player.sendSystemMessage(Component.literal("Seconds left: " + (time / SharedConstants.TICKS_PER_SECOND) + " | Hits: " + state.hits), true);

		if (time == 0) {
			return TickResult.CANCEL;
		}

		for (TeleportTarget teleporter : teleporters) {
			teleporter.tryTeleport(game, entity);
		}

		return TickResult.CONTINUE;
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

	private enum TickResult {
		CONTINUE,
		WIN,
		CANCEL,
	}

	private static class PlayerHoleState {
		private final GameType initialGameMode;
		private final LivingEntity crab;
		private int ticksLeft;
		private int hits;

		private PlayerHoleState(GameType initialGameMode, LivingEntity crab, int totalTicks) {
			this.initialGameMode = initialGameMode;
			this.crab = crab;
			ticksLeft = totalTicks;
		}

		public void discard() {
			crab.discard();
		}

		public void stopGame(@Nullable ServerPlayer player) {
			crab.setHealth(-100);
			crab.kill((ServerLevel) crab.level());
			if (player != null && player.gameMode() == FORCED_GAME_MODE) {
				player.setGameMode(initialGameMode);
			}
		}
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
