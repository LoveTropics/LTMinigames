package org.lovetropics.games.common.content.hoedown;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;

public record DerbyFireworksBehavior(
		float radius,
		float verticalKnockback,
		float horizontalKnockback
) implements IGameBehavior {
	public static final MapCodec<DerbyFireworksBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.floatRange(0.0f, 16.0f).fieldOf("radius").forGetter(DerbyFireworksBehavior::radius),
			Codec.floatRange(0.0f, 16.0f).fieldOf("vertical_knockback").forGetter(DerbyFireworksBehavior::verticalKnockback),
			Codec.floatRange(0.0f, 16.0f).fieldOf("horizontal_knockback").forGetter(DerbyFireworksBehavior::horizontalKnockback)
	).apply(i, DerbyFireworksBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameWorldEvents.PROJECTILE_IMPACT, (level, projectile, _) -> {
			if (projectile instanceof FireworkRocketEntity rocket) {
				for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, rocket.getBoundingBox().inflate(radius))) {
					Entity vehicle = player.getVehicle();
					if (vehicle != null) {
						player.stopRiding();
						vehicle.kill(level);
					}
					RandomSource random = player.getRandom();
					Vec3 impulse = new Vec3(
							random.nextFloat() * 2.0 - 1.0,
							1.0,
							random.nextFloat() * 2.0 - 1.0
					).multiply(horizontalKnockback, verticalKnockback, horizontalKnockback);
					player.setDeltaMovement(player.getKnownMovement().add(impulse));
					player.hurtMarked = true;
				}
			}
		});
	}

	@Override
	public MapCodec<DerbyFireworksBehavior> codec() {
		return CODEC;
	}
}
