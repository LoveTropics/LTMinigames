package com.lovetropics.minigames.common.core.game;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;
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
	@Nullable
	private CompoundTag loadFromTag;
	private final List<Consumer<ServerPlayer>> initializers = new ArrayList<>();

	public SpawnBuilder(final ServerPlayer player) {
		level = player.level();
		position = player.position();
		yRot = player.getYRot();
		xRot = player.getXRot();
	}

	public void teleportTo(final ServerLevel level, final Vec3 position, final float yRot, final float xRot) {
		this.level = level;
		this.position = position;
		this.yRot = yRot;
		this.xRot = xRot;
	}

	public void teleportTo(final ServerLevel level, final Vec3 position) {
		teleportTo(level, position, 0.0f, 0.0f);
	}

	public void teleportTo(final ServerLevel level, final BlockPos pos, final Direction forward) {
		teleportTo(level, pos, forward.toYRot());
	}

	public void teleportTo(final ServerLevel level, final BlockPos pos, float yRot) {
		teleportTo(level, Vec3.atBottomCenterOf(pos), yRot, 0.0f);
	}

	public void teleportTo(final ServerLevel level, final BlockPos pos) {
		teleportTo(level, pos, Direction.SOUTH);
	}

	public void setGameMode(final GameType gameType) {
		run(player -> player.setGameMode(gameType));
	}

	public void loadFromTag(CompoundTag loadFromTag) {
		this.loadFromTag = loadFromTag;
	}

	public void run(final Consumer<ServerPlayer> initializer) {
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

	public void teleportAndApply(final ServerPlayer player) {
		player.teleportTo(level, position.x, position.y, position.z, Set.of(), yRot, xRot, true);
		player.connection.resetPosition();
		applyInitializers(player);
	}

	public void loadInto(final ServerPlayer player) {
		if (loadFromTag != null) {
			try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
				ValueInput input = TagValueInput.create(reporter, player.registryAccess(), loadFromTag);
				player.load(input);
				player.loadGameTypes(input);
			}
		}
	}

	public void applyInitializers(final ServerPlayer player) {
		for (Consumer<ServerPlayer> initializer : initializers) {
			initializer.accept(player);
		}
	}
}
