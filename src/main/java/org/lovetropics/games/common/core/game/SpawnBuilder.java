package org.lovetropics.games.common.core.game;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SpawnBuilder {
	private static final Logger LOGGER = LogUtils.getLogger();

	private ServerLevel level;
	private Vec3 position;
	private float yRot;
	private float xRot;
	private final List<Consumer<ServerPlayer>> initializers = new ArrayList<>();

	public SpawnBuilder(ServerPlayer player) {
		level = player.level();
		position = player.position();
		yRot = player.getYRot();
		xRot = player.getXRot();
	}

	public void teleportTo(ServerLevel level, Vec3 position, float yRot, float xRot) {
		this.level = level;
		this.position = position;
		this.yRot = yRot;
		this.xRot = xRot;
	}

	public void teleportTo(ServerLevel level, Vec3 position) {
		teleportTo(level, position, 0.0f, 0.0f);
	}

	public void teleportTo(ServerLevel level, BlockPos pos, Direction forward) {
		teleportTo(level, pos, forward.toYRot());
	}

	public void teleportTo(ServerLevel level, BlockPos pos, float yRot) {
		teleportTo(level, Vec3.atBottomCenterOf(pos), yRot, 0.0f);
	}

	public void teleportTo(ServerLevel level, BlockPos pos) {
		teleportTo(level, pos, Direction.SOUTH);
	}

	public void setGameMode(GameType gameType) {
		run(player -> player.setGameMode(gameType));
	}

	public void run(Consumer<ServerPlayer> initializer) {
		initializers.add(initializer);
	}

	public ServerLevel level() {
		return level;
	}

	public Vec3 position() {
		return position;
	}

	public float yRot() {
		return yRot;
	}

	public float xRot() {
		return xRot;
	}

	public void teleportAndApply(ServerPlayer player) {
		player.teleportTo(level, position.x, position.y, position.z, Set.of(), yRot, xRot, true);
		player.connection.resetPosition();
		applyInitializers(player);
	}

	public void applyInitializers(ServerPlayer player) {
		for (Consumer<ServerPlayer> initializer : initializers) {
			initializer.accept(player);
		}
	}
}
