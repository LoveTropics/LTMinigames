package com.lovetropics.minigames.common.core.game.config;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Stores data-driven info about a minigame
 */
public record GameConfig(
		ResourceLocation id,
		ResourceLocation backendId,
		String statisticsKey,
		Component name,
		@Nullable Component subtitle,
		@Nullable ResourceLocation icon,
		int maximumParticipants,
		@Nullable ResourceLocation introSlideshow,
		@Nullable GamePhaseConfig waiting,
		GamePhaseConfig playing,
		boolean hideFromList
) implements IGameDefinition {
	public static Codec<GameConfig> codec(ResourceLocation id) {
		return RecordCodecBuilder.create(i -> i.group(
				ResourceLocation.CODEC.optionalFieldOf("backend_id").forGetter(c -> Optional.of(c.backendId)),
				Codec.STRING.optionalFieldOf("statistics_key").forGetter(c -> Optional.of(c.statisticsKey)),
				ComponentSerialization.CODEC.fieldOf("name").forGetter(c -> c.name),
				ComponentSerialization.CODEC.optionalFieldOf("subtitle").forGetter(c -> Optional.ofNullable(c.subtitle)),
				ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(c -> Optional.ofNullable(c.icon)),
				Codec.INT.optionalFieldOf("maximum_participants", 100).forGetter(c -> c.maximumParticipants),
				ResourceLocation.CODEC.optionalFieldOf("intro_slideshow").forGetter(c -> Optional.ofNullable(c.introSlideshow)),
				GamePhaseConfig.CODEC.optionalFieldOf("waiting").forGetter(c -> Optional.ofNullable(c.waiting)),
				GamePhaseConfig.MAP_CODEC.forGetter(c -> c.playing),
				Codec.BOOL.optionalFieldOf("hide_from_list", false).forGetter(c -> c.hideFromList)
		).apply(i, (backendIdOpt, statisticsKeyOpt, name, subtitleOpt, iconOpt, maximumParticipants, introSlideshowOpt, waitingOpt, active, hideFromList) -> {
			ResourceLocation backendId = backendIdOpt.orElse(id);
			String statisticsKey = statisticsKeyOpt.orElse(id.getPath());
			Component subtitle = subtitleOpt.orElse(null);
			ResourceLocation icon = iconOpt.orElse(null);
			ResourceLocation introSlideshow = introSlideshowOpt.orElse(null);
			GamePhaseConfig waiting = waitingOpt.orElse(null);
			return new GameConfig(id, backendId, statisticsKey, name, subtitle, icon, maximumParticipants, introSlideshow, waiting, active, hideFromList);
		}));
	}

	@Override
	public int getMaximumParticipantCount() {
		return maximumParticipants;
	}

	@Override
	public IGamePhaseDefinition getPlayingPhase() {
		return playing;
	}

	@Override
	public Optional<IGamePhaseDefinition> getWaitingPhase() {
		return Optional.ofNullable(waiting);
	}
}
