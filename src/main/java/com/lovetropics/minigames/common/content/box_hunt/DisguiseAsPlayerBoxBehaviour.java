package com.lovetropics.minigames.common.content.box_hunt;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.phys.Vec3;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;

import java.util.Optional;

public class DisguiseAsPlayerBoxBehaviour implements IGameBehavior {
	public static final MapCodec<DisguiseAsPlayerBoxBehaviour> CODEC = MapCodec.unit(DisguiseAsPlayerBoxBehaviour::new);
	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, player) -> {
			EntityDisguiseHolder disguiseHolder = EntityDisguiseHolder.getOrNull(player);
			if (disguiseHolder == null) {
				return false;
			}
			CompoundTag entityData = new CompoundTag();
			CompoundTag blockState = new CompoundTag();
			blockState.putString("Name", "ltextras:word_box");
			CompoundTag components = new CompoundTag();
			Component name = player.getName();
			components.put("minecraft:custom_name", ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, name).getOrThrow());
			entityData.put("BlockState", blockState);
			CompoundTag tileData = new CompoundTag();
			tileData.put("components", components);
			entityData.put("TileEntityData", tileData);
			Disguise boxDisguise = new Disguise(Optional.of(TypedEntityData.of(
					EntityType.FALLING_BLOCK,
					entityData
			)), 1.0f, false, Optional.empty(), Optional.empty(), true);
			disguiseHolder.set(boxDisguise);
			EntityDisguiseHolder.update(player, disguise -> boxDisguise);
			return true;
		});
		events.listen(GamePhaseEvents.TICK, () -> tick(game));
	}

	private static void tick(IGamePhase game) {
		for (ServerPlayer participant : game.participants()) {
			if (participant.isShiftKeyDown()) {
				EntityDisguiseHolder disguise = EntityDisguiseHolder.getOrNull(participant);
				if (disguise != null && disguise.disguise().entity().isPresent()) {
					Optional<TypedEntityData<EntityType<?>>> entity = disguise.disguise().entity();
					if (entity.get().type().equals(EntityType.FALLING_BLOCK)) {
						Vec3 vec3 = Vec3.atBottomCenterOf(participant.blockPosition());
						participant.teleportTo(vec3.x, vec3.y, vec3.z);
					}
				}
			}
		};
	}
}
