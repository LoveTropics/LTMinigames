package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;

import java.util.Optional;

public record ShowTitleAction(Optional<TemplatedText> title, Optional<TemplatedText> subtitle, Optional<TemplatedText> actionBar, int fadeIn, int stay, int fadeOut) implements IGameBehavior {
	public static final MapCodec<ShowTitleAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TemplatedText.CODEC.optionalFieldOf("title").forGetter(ShowTitleAction::title),
			TemplatedText.CODEC.optionalFieldOf("subtitle").forGetter(ShowTitleAction::subtitle),
			TemplatedText.CODEC.optionalFieldOf("action_bar").forGetter(ShowTitleAction::actionBar),
			Codec.INT.optionalFieldOf("fade_in", SharedConstants.TICKS_PER_SECOND / 4).forGetter(ShowTitleAction::fadeIn),
			Codec.INT.optionalFieldOf("stay", SharedConstants.TICKS_PER_SECOND * 2).forGetter(ShowTitleAction::stay),
			Codec.INT.optionalFieldOf("fade_out", SharedConstants.TICKS_PER_SECOND / 4).forGetter(ShowTitleAction::fadeOut)
	).apply(i, ShowTitleAction::new));

	private static final Component EMPTY_TITLE = CommonComponents.space();

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, target) -> {
			target.connection.send(new ClientboundClearTitlesPacket(true));
			target.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
			target.connection.send(new ClientboundSetTitleTextPacket(title.map(title -> title.apply(context)).orElse(EMPTY_TITLE)));
			subtitle.ifPresent(subtitle -> target.connection.send(new ClientboundSetSubtitleTextPacket(subtitle.apply(context))));
			actionBar.ifPresent(actionBar -> target.connection.send(new ClientboundSetActionBarTextPacket(actionBar.apply(context))));
			return true;
		});
	}
}
