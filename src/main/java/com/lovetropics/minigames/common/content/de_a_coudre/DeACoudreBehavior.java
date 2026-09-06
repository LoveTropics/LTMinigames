package com.lovetropics.minigames.common.content.de_a_coudre;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableBoolean;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record DeACoudreBehavior(
		String waterRegionKey,
		String holeRegion,
		BlockPalette defaultPalette,
		Map<GameTeamKey, BlockPalette> teamPalettes,
		int pointsPerBlock,
		int pointsPerBonus
) implements IGameBehavior {
	public static final MapCodec<DeACoudreBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("water_region").forGetter(b -> b.waterRegionKey),
			Codec.STRING.fieldOf("hole_region").forGetter(b -> b.holeRegion),
			BlockPalette.CODEC.optionalFieldOf("palette", BlockPalette.DEFAULT).forGetter(b -> b.defaultPalette),
			Codec.unboundedMap(GameTeamKey.CODEC, BlockPalette.CODEC).optionalFieldOf("team_palettes", Map.of()).forGetter(b -> b.teamPalettes),
			Codec.INT.optionalFieldOf("points_per_block", 1).forGetter(b -> b.pointsPerBlock),
			Codec.INT.optionalFieldOf("points_per_bonus", 3).forGetter(b -> b.pointsPerBonus)
	).apply(i, DeACoudreBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		ServerLevel level = game.level();
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);

		BlockBox water = game.mapRegions().getOrThrow(waterRegionKey);
		BlockBox hole = game.mapRegions().getOrThrow(holeRegion);
		int holeY = hole.min().getY();

		Board board = Board.build(level, water);

		MutableBoolean gameOver = new MutableBoolean();
		events.listen(GamePlayerEvents.TICK, player -> {
			if (gameOver.isTrue()) {
				return;
			}
			if (!player.onGround() || player.getY() >= holeY) {
				return;
			}

			BlockPos blockPos = player.blockPosition();
			GameTeamKey team = teams != null ? teams.getTeamForPlayer(player) : null;

			FillType type = board.tryFill(blockPos);
			onPlayerLanded(game, player, team, type, blockPos);

			if (board.isFilled()) {
				gameOver.setTrue();
				if (!game.invoker(GameLogicEvents.REQUEST_GAME_OVER).requestGameOver()) {
					game.requestStop(GameStopReason.finished());
				}
			}
		});
	}

	private void onPlayerLanded(IGamePhase game, ServerPlayer player, @Nullable GameTeamKey team, FillType type, BlockPos blockPos) {
		respawnPlayer(game, player);

		BlockPalette palette = team != null ? teamPalettes.getOrDefault(team, defaultPalette) : defaultPalette;

		int points = switch (type) {
			case NONE -> {
				com.lovetropics.minigames.common.util.Util.sendNotifySound(player, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 1.0f);
				yield 0;
			}
			case NORMAL -> {
				BlockState block = palette.blocks.get(Mth.abs(player.getUUID().hashCode()) % palette.blocks.size());
				game.level().setBlockAndUpdate(blockPos, block);
				com.lovetropics.minigames.common.util.Util.sendNotifySound(player, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, game.random().nextFloat() * 0.5f + 0.75f);
				yield pointsPerBlock;
			}
			case BONUS -> {
				game.level().setBlockAndUpdate(blockPos, palette.bonusBlock);
				com.lovetropics.minigames.common.util.Util.sendNotifySound(player, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
				yield pointsPerBonus;
			}
		};

		if (team != null) {
			game.statistics().forTeam(team).incrementInt(StatisticKey.POINTS, points);
		} else {
			game.statistics().forPlayer(player).incrementInt(StatisticKey.POINTS, points);
		}
	}

	private static void respawnPlayer(IGamePhase game, ServerPlayer player) {
		SpawnBuilder spawn = new SpawnBuilder(player);
		game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, PlayerRole.PARTICIPANT);
		spawn.teleportAndApply(player);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return DeACoudre.BEHAVIOR;
	}

	private static class Board {
		private final LongSet remainingWater = new LongOpenHashSet();

		public static Board build(ServerLevel level, BlockBox water) {
			Board board = new Board();
			for (BlockPos pos : water) {
				BlockState blockState = level.getBlockState(pos);
				if (blockState.is(Blocks.WATER)) {
					board.remainingWater.add(pos.asLong());
				}
			}
			return board;
		}

		public boolean isFilled() {
			return remainingWater.isEmpty();
		}

		public FillType tryFill(BlockPos pos) {
			if (!remainingWater.remove(pos.asLong())) {
				return FillType.NONE;
			}
			return isSurroundedByBlocks(pos) ? FillType.BONUS : FillType.NORMAL;
		}

		private boolean isSurroundedByBlocks(BlockPos pos) {
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				if (remainingWater.contains(pos.relative(direction).asLong())) {
					return false;
				}
			}
			return true;
		}
	}

	public enum FillType {
		NONE,
		NORMAL,
		BONUS,
	}

	public record BlockPalette(List<BlockState> blocks, BlockState bonusBlock) {
		public static final Codec<BlockPalette> CODEC = RecordCodecBuilder.create(i -> i.group(
				MoreCodecs.BLOCK_STATE.listOf().fieldOf("blocks").forGetter(BlockPalette::blocks),
				MoreCodecs.BLOCK_STATE.fieldOf("bonus").forGetter(BlockPalette::bonusBlock)
		).apply(i, BlockPalette::new));

		public static final BlockPalette DEFAULT = new BlockPalette(
				List.of(Blocks.CONCRETE.purple().defaultBlockState()),
				Blocks.EMERALD_BLOCK.defaultBlockState()
		);
	}
}
