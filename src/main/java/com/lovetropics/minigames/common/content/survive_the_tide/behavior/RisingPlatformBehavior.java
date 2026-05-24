package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import com.lovetropics.minigames.common.content.survive_the_tide.entity.PlatformEntity;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record RisingPlatformBehavior(
		String platformKey,
		int time,
		float height,
		BlockState blockState
) implements IGameBehavior {
	public static final MapCodec<RisingPlatformBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("platform").forGetter(RisingPlatformBehavior::platformKey),
			Codec.INT.fieldOf("time").forGetter(RisingPlatformBehavior::time),
			Codec.FLOAT.fieldOf("height").forGetter(RisingPlatformBehavior::height),
			MoreCodecs.BLOCK_STATE.fieldOf("block_state").forGetter(RisingPlatformBehavior::blockState)
	).apply(i, RisingPlatformBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox platformRegion = game.mapRegions().getOrThrow(platformKey);
		Vec3 center = platformRegion.center();
		double startY = platformRegion.min().getY();

		events.listen(GamePhaseEvents.START, initiator -> {
			PlatformEntity platform = new PlatformEntity(SurviveTheTide.PLATFORM.get(), game.level());
			platform.absSnapTo(center.x(), startY, center.z());
			platform.setWidth(platformRegion.size().getX());
			platform.setBlockState(blockState);

			game.level().addFreshEntity(platform);

			for (ServerPlayer participant : game.participants()) {
				participant.startRiding(platform, true, false);
			}

			platform.lerpTo(center.x(), startY + height, center.z(), time);
		});
	}
}
