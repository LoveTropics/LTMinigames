package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.region.LevelBlockPos;
import org.lovetropics.games.common.core.game.region.RegionQuery;

import java.util.Set;

public record TeleportToRegionAction(
		RegionQuery region
) implements IGameBehavior {
	public static final MapCodec<TeleportToRegionAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("region").forGetter(TeleportToRegionAction::region)
	).apply(i, TeleportToRegionAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToEntities(game, (_, _, target) -> {
			LevelBlockPos pos = this.region.pickRandomPos(game, game.random());
			if (pos == null) {
				return false;
			}
			Vec3 center = Vec3.atCenterOf(pos.pos());
			target.teleportTo(pos.level(), center.x, center.y, center.z, Set.of(), target.getYRot(), target.getXRot(), false);
			return true;
		});
	}
}
