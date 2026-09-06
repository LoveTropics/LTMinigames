package com.lovetropics.minigames.common.content.block_party;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.lib.entity.FireworkPalette;
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
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
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
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class BlockPartyBehavior implements IGameBehavior {
	public static final MapCodec<BlockPartyBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("floor").forGetter(c -> c.floorRegionKey),
			MoreCodecs.arrayOrUnit(MoreCodecs.BLOCK_STATE, BlockState[]::new).fieldOf("blocks").forGetter(c -> c.blocks),
			Codec.INT.optionalFieldOf("quad_size", 3).forGetter(c -> c.quadSize),
			Codec.INT.optionalFieldOf("max_time", SharedConstants.TICKS_PER_SECOND * 5).forGetter(c -> c.maxTime),
			Codec.INT.optionalFieldOf("min_time", SharedConstants.TICKS_PER_SECOND * 2).forGetter(c -> c.minTime),
			Codec.INT.optionalFieldOf("time_decay_rounds", 5).forGetter(c -> c.timeDecayRounds),
			Codec.INT.optionalFieldOf("interval", SharedConstants.TICKS_PER_SECOND * 3).forGetter(c -> c.interval),
			Codec.INT.optionalFieldOf("knockback_after_round", Integer.MAX_VALUE).forGetter(c -> c.knockbackAfterAround),
			Codec.INT.optionalFieldOf("max_lives", 1).forGetter(c -> c.maxLives)
	).apply(i, BlockPartyBehavior::new));

	private final String floorRegionKey;
	private final BlockState[] blocks;
	private final int quadSize;

	private final int maxTime;
	private final int minTime;
	private final int timeDecayRounds;
	private final int interval;
	private final int knockbackAfterAround;
	private final int maxLives;

	private IGamePhase game;
	private FloorRegion floorRegion;

	private @Nullable State state;

	public BlockPartyBehavior(String floorRegionKey, BlockState[] blocks, int quadSize, int maxTime, int minTime, int timeDecayRounds, int interval, int knockbackAfterAround, int maxLives) {
		this.floorRegionKey = floorRegionKey;
		this.blocks = blocks;
		this.quadSize = quadSize;
		this.maxTime = maxTime;
		this.minTime = minTime;
		this.timeDecayRounds = timeDecayRounds;
		this.interval = interval;
		this.knockbackAfterAround = knockbackAfterAround;
		this.maxLives = maxLives;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;

		if (blocks.length == 0) {
			throw new GameException(Component.literal("No blocks defined!"));
		}

		floorRegion = new FloorRegion(game.level(), game.mapRegions().getOrThrow(floorRegionKey), quadSize);

		events.listen(GamePhaseEvents.START, initiator -> {
			state = startCountingDown(0);
		});

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (role == PlayerRole.PARTICIPANT) {
				game.statistics().forPlayer(player).set(StatisticKey.LIVES, maxLives);
			}
		});
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(spawn));

		events.listen(GamePhaseEvents.TICK, this::tick);

		events.listen(GamePlayerEvents.DAMAGE_AMOUNT, (player, damageSource, amount, originalAmount) -> {
			if (damageSource.getEntity() instanceof Player) {
				return hasKnockback(state) ? 0.0f : amount;
			}
			return amount;
		});
		events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> {
			if (damageSource.getEntity() instanceof Player) {
				return hasKnockback(state) ? TriState.DEFAULT : TriState.FALSE;
			}
			return TriState.DEFAULT;
		});
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> target instanceof Player && !hasKnockback(state) ? TriState.FALSE : TriState.DEFAULT);

		events.listen(GamePlayerEvents.DEATH, (player, damageSource) -> {
			StatisticsMap playerStatistics = game.statistics().forPlayer(player);
			int lives = playerStatistics.getInt(StatisticKey.LIVES);
			int newLives = Math.max(lives - 1, 0);
			if (newLives == 0) {
				// Pass through to normal death logic
				return TriState.DEFAULT;
			}
			playerStatistics.set(StatisticKey.LIVES, newLives);
			SpawnBuilder spawn = new SpawnBuilder(player);
			spawnPlayer(spawn);
			player.setGameMode(GameType.SPECTATOR);
			spawn.teleportAndApply(player);
			return TriState.FALSE;
		});

		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			game.allPlayers().playSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
			state = new Ending(game.ticks() + SharedConstants.TICKS_PER_SECOND * 5);
		});
	}

	private boolean hasKnockback(@Nullable State state) {
		return state != null && state.hasKnockback();
	}

	private void spawnPlayer(SpawnBuilder spawn) {
		BlockPos floorPos = floorRegion.box.sample(game.level().getRandom());
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
		}
	}

	CountingDown startCountingDown(int round) {
		ServerLevel level = game.level();
		Floor floor = floorRegion.generateAndSet(level, level.getRandom(), blocks);

		ItemStack targetStack = new ItemStack(floor.target.getBlock());

		Component name = BlockPartyTexts.STAND_ON_BLOCK.apply(targetStack.getHoverName())
				.withStyle(style -> style.withBold(true));
		targetStack.set(DataComponents.ITEM_NAME, name);

		for (ServerPlayer player : game.participants()) {
			player.getInventory().clearContent();
			for (int i = 0; i < 9; i++) {
				player.getInventory().setItem(i, targetStack.copy());
			}

			if (player.gameMode() != GameType.SPECTATOR) {
				game.statistics().forPlayer(player).incrementInt(StatisticKey.ROUNDS_SURVIVED, 1);
			} else {
				SpawnBuilder spawn = new SpawnBuilder(player);
				spawnPlayer(spawn);
				player.setGameMode(GameType.ADVENTURE);
				spawn.teleportAndApply(player);
			}
		}

		float alpha = 1.0f - Math.min((float) round / timeDecayRounds, 1.0f);
		long duration = Mth.lerpInt(alpha, minTime, maxTime);
		return new CountingDown(round, game.ticks() + duration, floor);
	}

	Interval startInterval(int round, Floor floor) {
		floor.removeNonTargets(game.level(), floorRegion.box);
		return new Interval(round, game.ticks() + interval);
	}

	interface State {
		@Nullable State tick(IGamePhase game);

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
				int color = ARGB.linearLerp(
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

	private static class FloorRegion {
		private final BlockBox box;
		private final int quadSize;
		private final int quadCountX;
		private final int quadCountZ;
		private final BitSet mask;

		private FloorRegion(ServerLevel level, BlockBox box, int quadSize) {
			this.box = box;
			this.quadSize = quadSize;
			quadCountX = Mth.positiveCeilDiv(box.size().getX(), quadSize);
			quadCountZ = Mth.positiveCeilDiv(box.size().getZ(), quadSize);
			mask = new BitSet(quadCountX * quadCountZ);
			mask.set(0, quadCountX * quadCountZ);
			for (BlockPos pos : box) {
				if (level.isEmptyBlock(pos)) {
					int quadX = (pos.getX() - box.min().getX()) / quadSize;
					int quadZ = (pos.getZ() - box.min().getZ()) / quadSize;
					mask.clear(quadX + quadZ * quadCountX);
				}
			}
		}

		public Floor generateAndSet(ServerLevel level, RandomSource random, BlockState[] blocks) {
			BlockState[] quads = new BlockState[quadCountX * quadCountZ];

			List<BlockState> candidateTargets = new ArrayList<>();

			for (BlockPos pos : box) {
				int quadX = (pos.getX() - box.min().getX()) / quadSize;
				int quadZ = (pos.getZ() - box.min().getZ()) / quadSize;

				int quadIndex = quadX + quadZ * quadCountX;
				if (!mask.get(quadIndex)) {
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
					continue;
				}

				BlockState selectedBlock = quads[quadIndex];
				if (selectedBlock == null) {
					selectedBlock = Util.getRandom(blocks, random);
					quads[quadIndex] = selectedBlock;
					if (!candidateTargets.contains(selectedBlock)) {
						candidateTargets.add(selectedBlock);
					}
				}

				level.setBlock(pos, selectedBlock, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
			}

			BlockState target = Util.getRandom(candidateTargets, random);
			return new Floor(target);
		}
	}

	record Floor(BlockState target) {
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
