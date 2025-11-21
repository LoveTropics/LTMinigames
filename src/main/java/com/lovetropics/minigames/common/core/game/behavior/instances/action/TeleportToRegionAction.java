package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

public record TeleportToRegionAction(
		String region
) implements IGameBehavior {

	public static final MapCodec<TeleportToRegionAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(TeleportToRegionAction::region)
	).apply(i, TeleportToRegionAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox teleportToRegion = game.mapRegions().getAny(region);
		if(teleportToRegion == null){
			return;
		}
		Vec3 center = teleportToRegion.center();
		events.applyToEntities(game, (context, target) -> {
			target.teleportTo(center.x, center.y, center.z);
			return true;
		});
	}
}
