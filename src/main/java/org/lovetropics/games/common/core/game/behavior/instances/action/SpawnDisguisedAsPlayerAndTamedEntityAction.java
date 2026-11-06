package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.util.EntityTemplate;
import org.lovetropics.games.common.util.Util;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;

import java.util.Optional;

public record SpawnDisguisedAsPlayerAndTamedEntityAction(EntityTemplate entity, double distance) implements IGameBehavior {

	public static final MapCodec<SpawnDisguisedAsPlayerAndTamedEntityAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityTemplate.CODEC.fieldOf("entity").forGetter(c -> c.entity),
			Codec.DOUBLE.optionalFieldOf("distance", 0.0).forGetter(c -> c.distance)
	).apply(i, SpawnDisguisedAsPlayerAndTamedEntityAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context,  target) -> {
			String sendingPlayer = context.getOrDefault(GameActionContextKeys.PACKAGE_SENDER, "LoveTropics");
			Vec3 spawnPos = findSpawnPos(target.level(), target);
			if (spawnPos == null) {
				spawnPos = target.position();
			}
			Entity spawn = entity.spawn(target.level(), spawnPos.x, spawnPos.y, spawnPos.z, 0.0f, 0.0f);
			if(spawn != null) {
				if (spawn instanceof TamableAnimal tamedAnimal) {
					tamedAnimal.setOwner(target);
					tamedAnimal.setTame(true, true);
					tamedAnimal.setCustomName(Component.literal(sendingPlayer));
					tamedAnimal.setCustomNameVisible(true);
				}
				EntityDisguiseHolder disguiseHolder = EntityDisguiseHolder.getOrNull(spawn);
				if (disguiseHolder != null) {
					CompoundTag root = new CompoundTag();
					CompoundTag profile = new CompoundTag();
					profile.putString("name", sendingPlayer);
					root.put("profile", profile);
					root.putBoolean("hide_description", true);
					Disguise mannequinDisguise = Disguise.of(EntityTypes.WOLF)
							.withCustomName(Optional.of(Component.literal(sendingPlayer)))
							.withEntity(Optional.of(TypedEntityData.of(EntityTypes.MANNEQUIN, root)))
							.withScale(0.6f);
					disguiseHolder.set(mannequinDisguise);
				}
			}
			return true;
		});
	}

	private @Nullable Vec3 findSpawnPos(ServerLevel level, Entity entity) {
		for (int i = 0; i < 10; i++) {
			double angle = entity.getRandom().nextDouble() * 2 * Math.PI;
			double x = entity.getX() + Math.sin(angle) * distance;
			double z = entity.getZ() + Math.cos(angle) * distance;
			int maxDistanceY = Mth.floor(distance);

			BlockPos groundPos = Util.findGround(level, BlockPos.containing(x, entity.getY(), z), maxDistanceY);
			if (groundPos != null) {
				return new Vec3(x, groundPos.getY(), z);
			}
		}

		return null;
	}
}
