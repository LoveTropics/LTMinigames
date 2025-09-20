package com.lovetropics.minigames.common.content.block_party;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.lib.entity.FireworkPalette;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class BlockPartyBehavior implements IGameBehavior {
	public static final MapCodec<BlockPartyBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("floor").forGetter(c -> c.floorRegionKey),
			MoreCodecs.arrayOrUnit(MoreCodecs.BLOCK_STATE, BlockState[]::new).fieldOf("blocks").forGetter(c -> c.blocks),
			Codec.INT.optionalFieldOf("quad_size", 3).forGetter(c -> c.quadSize),
			Codec.LONG.optionalFieldOf("max_time", 20L * 5).forGetter(c -> c.maxTime),
			Codec.LONG.optionalFieldOf("min_time", 20L * 2).forGetter(c -> c.minTime),
			Codec.INT.optionalFieldOf("time_decay_rounds", 5).forGetter(c -> c.timeDecayRounds),
			Codec.LONG.optionalFieldOf("interval", 20L * 3).forGetter(c -> c.interval),
			Codec.INT.optionalFieldOf("knockback_after_round", Integer.MAX_VALUE).forGetter(c -> c.knockbackAfterAround)
	).apply(i, BlockPartyBehavior::new));

	private final String floorRegionKey;
	private final BlockState[] blocks;
	private final int quadSize;

	private final long maxTime;
	private final long minTime;
	private final int timeDecayRounds;
	private final long interval;
	private final int knockbackAfterAround;

	private IGamePhase game;
	private BlockBox floorRegion;

	private int quadCountX;
	private int quadCountZ;

	@Nullable
	private State state;

	public BlockPartyBehavior(String floorRegionKey, BlockState[] blocks, int quadSize, long maxTime, long minTime, int timeDecayRounds, long interval, int knockbackAfterAround) {
		this.floorRegionKey = floorRegionKey;
		this.blocks = blocks;
		this.quadSize = quadSize;
		this.maxTime = maxTime;
		this.minTime = minTime;
		this.timeDecayRounds = timeDecayRounds;
		this.interval = interval;
		this.knockbackAfterAround = knockbackAfterAround;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;

		if (blocks.length == 0) {
			throw new GameException(Component.literal("No blocks defined!"));
		}

		floorRegion = game.mapRegions().getOrThrow(floorRegionKey);
		BlockPos floorSize = floorRegion.size();
		quadCountX = floorSize.getX() / quadSize;
		quadCountZ = floorSize.getZ() / quadSize;

		events.listen(GamePhaseEvents.START, () -> {
			state = startCountingDown(0);
		});

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(spawn));

		events.listen(GamePhaseEvents.TICK, this::tick);

		events.listen(GamePlayerEvents.DAMAGE_AMOUNT, (player, damageSource, amount, originalAmount) -> hasKnockback(state) ? 0.0f : amount);
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> target instanceof Player && !hasKnockback(state) ? TriState.FALSE : TriState.DEFAULT);
		events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> hasKnockback(state) ? TriState.DEFAULT : TriState.FALSE);

		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			game.allPlayers().playSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
			state = new Ending(game.ticks() + SharedConstants.TICKS_PER_SECOND * 5);
		});
	}

	private boolean hasKnockback(@Nullable State state) {
		return state != null && state.hasKnockback();
	}

	private void spawnPlayer(SpawnBuilder spawn) {
		BlockPos floorPos = floorRegion.sample(game.level().getRandom());
		spawn.teleportTo(game.level(), floorPos.above());
	}

	private void onStateChange(State oldState, @Nullable State newState) {
		if (hasKnockback(newState) && !hasKnockback(oldState)) {
			PlayerSet allPlayers = game.allPlayers();
			allPlayers.showTitle(BlockPartyTexts.KNOCKBACK_ENABLED_TITLE, BlockPartyTexts.KNOCKBACK_ENABLED_SUBTITLE, 10, 40, 10);
			allPlayers.playSound(SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
		}
	}

	private void tick() {
		if (state == null) {
			return;
		}

		State newState = state.tick(game);
		if (state != newState) {
			onStateChange(state, newState);
		}
		state = newState;

		if (newState == null) {
			game.requestStop(GameStopReason.finished());
			return;
		}

		List<ServerPlayer> eliminated = new ArrayList<>();
		PlayerSet participants = game.participants();
		for (ServerPlayer player : participants) {
			double y = player.getY();
			if (y < player.level().getMinY() || y < floorRegion.min().getY() - 10) {
				eliminated.add(player);
			}
		}

		for (ServerPlayer player : eliminated) {
			game.setPlayerRole(player, PlayerRole.SPECTATOR);
			game.allPlayers().playSound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.0f);
			game.allPlayers().sendMessage(MinigameTexts.ELIMINATED.apply(player.getDisplayName()));
		}
	}

	CountingDown startCountingDown(int round) {
		ServerLevel world = game.level();
		Floor floor = Floor.generate(world.random, quadCountX, quadCountZ, blocks);
		floor.set(world, floorRegion, quadSize);

		ItemStack targetStack = new ItemStack(floor.target.getBlock());

		Component name = BlockPartyTexts.STAND_ON_BLOCK.apply(targetStack.getHoverName())
				.withStyle(style -> style.withBold(true));
		targetStack.set(DataComponents.ITEM_NAME, name);

		for (ServerPlayer player : game.participants()) {
			player.getInventory().clearContent();
			for (int i = 0; i < 9; i++) {
				player.getInventory().setItem(i, targetStack.copy());
			}
			game.statistics().forPlayer(player).set(StatisticKey.ROUNDS_SURVIVED, round);
		}

		float lerp = (float) round / timeDecayRounds;
		long duration = Mth.floor(Mth.clampedLerp(maxTime, minTime, lerp));
		return new CountingDown(round + 1, game.ticks() + duration, floor);
	}

	Interval startInterval(int round, Floor floor) {
		floor.removeNonTargets(game.level(), floorRegion);
		return new Interval(round, game.ticks() + interval);
	}

	interface State {
		@Nullable
		State tick(IGamePhase game);

		boolean hasKnockback();
	}

	final class CountingDown implements State {
		private static final int FINAL_COUNTDOWN_SECONDS = 3;

		private final int round;
		private final long breakAt;

		private final Floor floor;

		CountingDown(int round, long breakAt, Floor floor) {
			this.round = round;
			this.breakAt = breakAt;
			this.floor = floor;
		}

		@Override
		public State tick(IGamePhase game) {
			PlayerSet players = game.allPlayers();
			long time = game.ticks();

			long ticksLeft = breakAt - time;
			if (ticksLeft <= 0) {
				players.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0f, 2.0f);
				return startInterval(round, floor);
			}

			long secondsLeft = ticksLeft / SharedConstants.TICKS_PER_SECOND;
			if (ticksLeft % 10 == 0) {
				Component message = BlockPartyTexts.BREAK_IN_SECONDS.apply(secondsLeft).withStyle(ChatFormatting.GOLD);
				players.sendMessage(message, true);
			}

			if (secondsLeft <= FINAL_COUNTDOWN_SECONDS && ticksLeft % SharedConstants.TICKS_PER_SECOND == 0) {
				players.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
				int color = ARGB.lerp(
						Mth.inverseLerp(secondsLeft, FINAL_COUNTDOWN_SECONDS, 1),
						0x55ff55, 0xffaa00
				);
				Component title = Component.literal(".." + secondsLeft).withStyle(Style.EMPTY.withColor(color));
				players.showTitle(title, 4, SharedConstants.TICKS_PER_SECOND, 4);
			}

			return this;
		}

		@Override
		public boolean hasKnockback() {
			return round >= knockbackAfterAround;
		}
	}

	final class Interval implements State {
		private final int round;
		private final long nextAt;

		Interval(int round, long nextAt) {
			this.round = round;
			this.nextAt = nextAt;
		}

		@Override
		public State tick(IGamePhase game) {
			long time = game.ticks();
			if (time > nextAt) {
				return startCountingDown(round + 1);
			}
			return this;
		}

		@Override
		public boolean hasKnockback() {
			return round >= knockbackAfterAround;
		}
	}

	record Ending(long endAt) implements State {
		@Override
		public State tick(IGamePhase game) {
			if (game.ticks() > endAt) {
				return null;
			}
			for (ServerPlayer player : game.participants()) {
				if (!player.isSpectator() && game.random().nextInt(SharedConstants.TICKS_PER_SECOND) == 0) {
					BlockPos fireworksPos = BlockPos.containing(player.getEyePosition()).above();
					FireworkPalette.DYE_COLORS.spawn(fireworksPos, game.level());
				}
			}
			return this;
		}

		@Override
		public boolean hasKnockback() {
			return false;
		}
	}

	static final class Floor {
		private final BlockState[] quads;
		private final int quadCountX;
		private final int quadCountZ;

		private final BlockState target;

		Floor(BlockState[] quads, int quadCountX, int quadCountZ, BlockState target) {
			this.quads = quads;
			this.quadCountX = quadCountX;
			this.quadCountZ = quadCountZ;
			this.target = target;
		}

		static Floor generate(RandomSource random, int quadCountX, int quadCountZ, BlockState[] blocks) {
			BlockState[] quads = new BlockState[quadCountX * quadCountZ];

			for (int z = 0; z < quadCountZ; z++) {
				for (int x = 0; x < quadCountX; x++) {
					quads[x + z * quadCountX] = blocks[random.nextInt(blocks.length)];
				}
			}

			BlockState target = Util.getRandom(quads, random);
			return new Floor(quads, quadCountX, quadCountZ, target);
		}

		void set(ServerLevel world, BlockBox box, int quadSize) {
			for (BlockPos pos : box) {
				int localX = pos.getX() - box.min().getX();
				int localZ = pos.getZ() - box.min().getZ();
				int x = Mth.clamp(localX / quadSize, 0, quadCountX - 1);
				int z = Mth.clamp(localZ / quadSize, 0, quadCountZ - 1);

				BlockState quad = quads[x + z * quadCountX];
				world.setBlock(pos, quad, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
			}
		}

		void removeNonTargets(ServerLevel world, BlockBox box) {
			for (BlockPos pos : box) {
				BlockState state = world.getBlockState(pos);
				if (!state.equals(target)) {
					world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
				}
			}
		}
	}
}
