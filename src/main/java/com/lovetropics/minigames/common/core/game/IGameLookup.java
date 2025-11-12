package com.lovetropics.minigames.common.core.game;

import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface IGameLookup {
	@Nullable
	GameLobby getLobbyFor(Player player);

	@Nullable
	default GameLobby getLobbyFor(CommandSourceStack source) {
		if (source.getEntity() instanceof Player player) {
			return getLobbyFor(player);
		}
		return null;
	}

	@Nullable
	IGamePhase getGamePhaseFor(Player player);

	@Nullable
	IGamePhase getGamePhaseAt(Level level, Vec3 pos);

	@Nullable
	IGamePhase getGamePhaseInDimension(Level level);

	@Nullable
	default IGamePhase getGamePhaseFor(CommandSourceStack source) {
		if (source.getEntity() instanceof Player player) {
			return getGamePhaseFor(player);
		}
		return getGamePhaseAt(source.getLevel(), source.getPosition());
	}

	@Nullable
	default IGamePhase getGamePhaseFor(Entity entity) {
		if (entity.level().isClientSide) {
			return null;
		}
		if (entity instanceof Player player) {
			return getGamePhaseFor(player);
		} else {
			return getGamePhaseAt(entity.level(), entity.position());
		}
	}

	@Nullable
	default IGamePhase getGamePhaseAt(Level level, BlockPos pos) {
		return getGamePhaseAt(level, Vec3.atCenterOf(pos));
	}
}
