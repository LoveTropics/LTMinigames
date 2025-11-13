package com.lovetropics.minigames.common.content.treasure_dig;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Map;

public record TreasureDigBehaviour(
		String chestsRegion,
		Map<Block, ResourceKey<LootTable>> lootTables,
		Map<Holder<Item>, Integer> pointsMap
) implements IGameBehavior {
	public static final MapCodec<TreasureDigBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("chests_region").forGetter(b -> b.chestsRegion),
			Codec.unboundedMap(BuiltInRegistries.BLOCK.byNameCodec(), ResourceKey.codec(Registries.LOOT_TABLE)).optionalFieldOf("loot_tables", Map.of()).forGetter(b -> b.lootTables),
			Codec.unboundedMap(BuiltInRegistries.ITEM.holderByNameCodec(), Codec.INT).optionalFieldOf("points_map", Map.of()).forGetter(b -> b.pointsMap)
	).apply(i, TreasureDigBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		ServerLevel level = game.level();
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		BlockBox chestRegion = game.mapRegions().getOrThrow(chestsRegion);

		// Rushes dodgy code for filling the chests
		for (long chunk : chestRegion.asChunks()) {
			LevelChunk levelChunk = level.getChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
			for (BlockPos pos : levelChunk.getBlockEntitiesPos()) {
				if (levelChunk.getBlockEntity(pos) instanceof ChestBlockEntity blockEntity) {
					ResourceKey<LootTable> lootTable = lootTables().get(blockEntity.getBlockState().getBlock());
					if (lootTable != null) {
						blockEntity.setLootTable(lootTable, level.random.nextLong());
					}
				}
			}
		}

		events.listen(GamePlayerEvents.INVENTORY_CHANGED, (player, container, slotIndex, newItemStack) -> {
			GameTeamKey team = teams.getTeamForPlayer(player);
			if (team == null) {
				return;
			}

			int oldPlayerScore = game.statistics().forPlayer(player).getInt(StatisticKey.POINTS);
			int newPlayerScore = calculatePlayerScore(player);
			if (newPlayerScore == oldPlayerScore) {
				return;
			}
			game.statistics().forPlayer(player).set(StatisticKey.POINTS, newPlayerScore);
			player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.0f);

			game.statistics().forTeam(team).set(StatisticKey.POINTS, sumTeamScore(game, teams, team));
		});
	}

	private int sumTeamScore(IGamePhase game, TeamState teams, GameTeamKey team) {
		int teamScore = 0;
		for (ServerPlayer player : teams.getPlayersForTeam(game, team)) {
			teamScore += game.statistics().forPlayer(player).getInt(StatisticKey.POINTS);
		}
		return teamScore;
	}

	private int calculatePlayerScore(ServerPlayer serverPlayer) {
		int score = 0;
		for (ItemStack itemStack : serverPlayer.getInventory()) {
			if (itemStack.isEmpty()) {
				continue;
			}
			score += pointsMap().getOrDefault(itemStack.getItemHolder(), 0) * itemStack.getCount();
		}
		return score;
	}
}
