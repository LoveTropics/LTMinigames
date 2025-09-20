package com.lovetropics.minigames.common.core.game.behavior.instances.donation;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.TriState;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.List;

public record BlockPackagesDuringPhaseBehavior(ProgressChannel channel, List<ProgressionPeriod> blockedPeriods) implements IGameBehavior {
	public static final MapCodec<BlockPackagesDuringPhaseBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(BlockPackagesDuringPhaseBehavior::channel),
			ExtraCodecs.compactListCodec(ProgressionPeriod.CODEC).fieldOf("block_periods").forGetter(BlockPackagesDuringPhaseBehavior::blockedPeriods)
	).apply(i, BlockPackagesDuringPhaseBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		ProgressHolder progression = channel.getOrThrow(game);

		MutableBoolean gameOver = new MutableBoolean();
		events.listen(GameLogicEvents.GAME_OVER, winner -> gameOver.setTrue());

		events.listen(GamePackageEvents.RECEIVE_PACKAGE, gamePackage -> {
			if (progression.is(blockedPeriods) || gameOver.isTrue()) {
				return TriState.FALSE;
			}
			return TriState.DEFAULT;
		});
	}
}
