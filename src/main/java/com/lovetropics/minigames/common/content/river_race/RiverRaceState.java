package com.lovetropics.minigames.common.content.river_race;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.river_race.block.HasTrivia;
import com.lovetropics.minigames.common.content.river_race.block.TriviaType;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RiverRaceState implements IGameState {
	public static final GameStateKey.Defaulted<RiverRaceState> KEY = GameStateKey.create("River Race", RiverRaceState::new);

	private final Map<GameTeamKey, BlockBox> teamRegions = new HashMap<>();
	private final List<Zone> zones = new ArrayList<>();
	private Direction forwardDirection = Direction.NORTH;

	private @Nullable Zone currentZone;

	public void setTeamRegion(GameTeamKey team, BlockBox region) {
		teamRegions.put(team, region);
	}

	public void setForwardDirection(Direction forwardDirection) {
		this.forwardDirection = forwardDirection;
	}

	private Map<BlockPos, TriviaType> findAllTriviaBlocks(IGamePhase game, BlockBox box) {
		Map<BlockPos, TriviaType> triviaBlocks = new HashMap<>();

		LongIterator chunkIterator = box.asChunks().longIterator();
		while (chunkIterator.hasNext()) {
			long chunkPos = chunkIterator.nextLong();
			LevelChunk chunk = game.level().getChunk(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
			for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
				BlockPos pos = entry.getKey();
				if (box.contains(pos) && entry.getValue() instanceof HasTrivia hasTrivia) {
					triviaBlocks.put(pos, hasTrivia.getTriviaType());
				}
			}
		}

		return triviaBlocks;
	}

	public void addZone(IGamePhase game, String id, BlockBox box, Component ordinalName, Component displayName, DyeColor color) {
		Map<BlockPos, TriviaType> triviaBlocks = findAllTriviaBlocks(game, box);
		Zone zone = new Zone(id, box, ordinalName, displayName, color, triviaBlocks);
		zones.add(zone);
		if (currentZone == null) {
			currentZone = zone;
		}
	}

	public void setCurrentZone(Zone currentZone) {
		this.currentZone = currentZone;
	}

	public Zone getZoneById(String id) {
		for (Zone zone : zones) {
			if (zone.id.equals(id)) {
				return zone;
			}
		}
		throw new IllegalArgumentException("No zone with id: '" + id + "'");
	}

	public @Nullable Zone getZoneByPos(BlockPos pos) {
		for (Zone zone : zones) {
			if (zone.box.contains(pos)) {
				return zone;
			}
		}
		return null;
	}

	public List<Zone> getZones() {
		return zones;
	}

	public Zone currentZone() {
		return Objects.requireNonNull(currentZone);
	}

	// Mirrored for the opposite team side so that equivalent trivia blocks can reuse the same question
	public ZoneLocalPos getZoneLocalPosKey(Zone zone, BlockPos pos) {
		pos = pos.subtract(zone.box.min());
		BlockPos size = zone.box.size();
		if (forwardDirection.getAxis() == Direction.Axis.X) {
			if (pos.getZ() >= size.getZ() / 2) {
				pos = new BlockPos(pos.getX(), pos.getY(), size.getZ() - 1 - pos.getZ());
			}
		} else {
			if (pos.getX() >= size.getX() / 2) {
				pos = new BlockPos(size.getX() - 1 - pos.getX(), pos.getY(), pos.getZ());
			}
		}
		return new ZoneLocalPos(zone, pos.asLong());
	}

	public @Nullable Zone getZoneWithCollectable(ItemStack itemStack) {
		for (Zone zone : zones) {
			ItemStack collectable = zone.collectable;
			if (collectable != null && ItemStack.isSameItemSameComponents(collectable, itemStack)) {
				return zone;
			}
		}
		return null;
	}

	public @Nullable GameTeamKey getTeamAt(BlockPos pos) {
		for (Map.Entry<GameTeamKey, BlockBox> entry : teamRegions.entrySet()) {
			if (entry.getValue().contains(pos)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public static final class Zone {
		private final String id;
		private final BlockBox box;
		private final Component ordinalName;
		private final Component displayName;
		private final DyeColor color;
		private final Map<BlockPos, TriviaType> triviaBlocks;

		private @Nullable ItemStack collectable;

		public Zone(String id, BlockBox box, Component ordinalName, Component displayName, DyeColor color, Map<BlockPos, TriviaType> triviaBlocks) {
			this.id = id;
			this.box = box;
			this.ordinalName = ordinalName;
			this.displayName = displayName;
			this.color = color;
			this.triviaBlocks = triviaBlocks;
		}

		public String id() {
			return id;
		}

		public BlockBox box() {
			return box;
		}

		public Component ordinalName() {
			return ordinalName;
		}

		public Component displayName() {
			return displayName;
		}

		public DyeColor color() {
			return color;
		}

		public Map<BlockPos, TriviaType> triviaBlocks() {
			return triviaBlocks;
		}

		public @Nullable ItemStack collectable() {
			return collectable;
		}

		public void setCollectable(ItemStack collectable) {
			this.collectable = collectable;
		}
	}

	public record ZoneLocalPos(
			Zone zone,
			long pos
	) {
	}
}
