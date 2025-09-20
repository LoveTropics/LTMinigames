package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.minigames.common.content.river_race.RiverRace;
import com.lovetropics.minigames.common.core.entity.MinigameEntities;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.UUID;
import java.util.function.Supplier;

public record KillAboveVoidBehavior(int ticks) implements IGameBehavior {
	public static final MapCodec<KillAboveVoidBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("ticks").forGetter(KillAboveVoidBehavior::ticks)
	).apply(i, KillAboveVoidBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Object2IntOpenHashMap<UUID> ticksAboveVoid = new Object2IntOpenHashMap<>();
		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.isSpectator() || player.isCreative()) {
				return;
			}
			if (isFullyAboveVoid(player)) {
				int oldTicks = ticksAboveVoid.addTo(player.getUUID(), 1);
				if (oldTicks == ticks) {
					// For style :)
					LightningBolt lightning = MinigameEntities.QUIET_LIGHTNING_BOLT.get().create(player.level(), EntitySpawnReason.COMMAND);
					lightning.snapTo(player.position());
					lightning.setVisualOnly(true);
					player.level().addFreshEntity(lightning);
					// Now die >:(
					game.scheduler().runAfterSeconds(0.15f, () ->
							player.hurtServer(player.level(), player.damageSources().outOfBorder(), Float.MAX_VALUE)
					);
				}
			} else {
				ticksAboveVoid.removeInt(player.getUUID());
			}
		});
	}

	private boolean isFullyAboveVoid(ServerPlayer player) {
		Level level = player.level();
		AABB boundingBox = player.getBoundingBox();
		int minX = Mth.floor(boundingBox.minX);
		int minZ = Mth.floor(boundingBox.minZ);
		int maxX = Mth.floor(boundingBox.maxX);
		int maxZ = Mth.floor(boundingBox.maxZ);
		for (int z = minZ; z <= maxZ; z++) {
			for (int x = minX; x <= maxX; x++) {
				int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
				if (height != level.getMinY()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return RiverRace.KILL_ABOVE_VOID_BEHAVIOR;
	}
}
