package com.lovetropics.minigames.common.core.game;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface IGameLookup {
	static IGameLookup get() {
		return Binder.INSTANCE;
	}

	@Nullable IGamePhase getGamePhaseFor(Player player);

	@Nullable IGamePhase getGamePhaseAt(Level level, Vec3 pos);

	@Nullable IGamePhase getGamePhaseInDimension(Level level);

	default @Nullable IGamePhase getGamePhaseFor(CommandSourceStack source) {
		if (source.getEntity() instanceof Player player) {
			return getGamePhaseFor(player);
		}
		return getGamePhaseAt(source.getLevel(), source.getPosition());
	}

	default @Nullable IGamePhase getGamePhaseFor(Entity entity) {
		if (entity.level().isClientSide()) {
			return null;
		}
		if (entity instanceof Player player) {
			return getGamePhaseFor(player);
		} else {
			return getGamePhaseAt(entity.level(), entity.position());
		}
	}

	default @Nullable IGamePhase getGamePhaseAt(Level level, BlockPos pos) {
		return getGamePhaseAt(level, Vec3.atCenterOf(pos));
	}

	class Binder implements IGameLookup {
		private static final Binder INSTANCE = new Binder();

		private @Nullable IGameLookup inner;

		public static void bind(IGameLookup lookup) {
			INSTANCE.inner = lookup;
		}

		@Override
		public @Nullable IGamePhase getGamePhaseFor(Player player) {
			return inner != null ? inner.getGamePhaseFor(player) : null;
		}

		@Override
		public @Nullable IGamePhase getGamePhaseAt(Level level, Vec3 pos) {
			return inner != null ? inner.getGamePhaseAt(level, pos) : null;
		}

		@Override
		public @Nullable IGamePhase getGamePhaseInDimension(Level level) {
			return inner != null ? inner.getGamePhaseInDimension(level) : null;
		}

		@Override
		public @Nullable IGamePhase getGamePhaseFor(CommandSourceStack source) {
			return inner != null ? inner.getGamePhaseFor(source) : null;
		}

		@Override
		public @Nullable IGamePhase getGamePhaseFor(Entity entity) {
			return inner != null ? inner.getGamePhaseFor(entity) : null;
		}

		@Override
		public @Nullable IGamePhase getGamePhaseAt(Level level, BlockPos pos) {
			return inner != null ? inner.getGamePhaseAt(level, pos) : null;
		}
	}
}
