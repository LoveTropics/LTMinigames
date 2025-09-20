package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.util.EntityTemplate;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public record SpawnEntityAtPlayerAction(EntityTemplate entity, int damagePlayerAmount, double distance) implements IGameBehavior {
	public static final MapCodec<SpawnEntityAtPlayerAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityTemplate.CODEC.fieldOf("entity").forGetter(c -> c.entity),
			Codec.INT.optionalFieldOf("damage_player_amount", 0).forGetter(c -> c.damagePlayerAmount),
			Codec.DOUBLE.optionalFieldOf("distance", 0.0).forGetter(c -> c.distance)
	).apply(i, SpawnEntityAtPlayerAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> {
			Vec3 spawnPos = findSpawnPos(game, player);
			if (spawnPos == null) {
				spawnPos = player.position();
			}

			entity.spawn(player.level(), spawnPos.x, spawnPos.y, spawnPos.z, 0.0f, 0.0f);
			if (damagePlayerAmount > 0) {
				player.hurtServer(player.level(), player.damageSources().generic(), damagePlayerAmount);
			}

			return true;
		});
	}

	@Nullable
	private Vec3 findSpawnPos(IGamePhase game, ServerPlayer player) {
		for (int i = 0; i < 10; i++) {
			double angle = player.getRandom().nextDouble() * 2 * Math.PI;
			double x = player.getX() + Math.sin(angle) * distance;
			double z = player.getZ() + Math.cos(angle) * distance;
			int maxDistanceY = Mth.floor(distance);

			BlockPos groundPos = Util.findGround(game.level(), BlockPos.containing(x, player.getY(), z), maxDistanceY);
			if (groundPos != null) {
				return new Vec3(x, groundPos.getY(), z);
			}
		}

		return null;
	}
}
