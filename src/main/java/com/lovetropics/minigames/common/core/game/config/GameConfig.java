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
		int minimumParticipants,
		int maximumParticipants,
		@Nullable ResourceLocation introSlideshow,
		@Nullable GamePhaseConfig waiting,
		GamePhaseConfig playing,
		boolean isMultiGame,
		boolean hideFromList
) implements IGameDefinition {
	public static Codec<GameConfig> codec(ResourceLocation id) {
		return RecordCodecBuilder.create(i -> i.group(
				ResourceLocation.CODEC.optionalFieldOf("backend_id").forGetter(c -> Optional.of(c.backendId)),
				Codec.STRING.optionalFieldOf("statistics_key").forGetter(c -> Optional.of(c.statisticsKey)),
				ComponentSerialization.CODEC.fieldOf("name").forGetter(c -> c.name),
				ComponentSerialization.CODEC.optionalFieldOf("subtitle").forGetter(c -> Optional.ofNullable(c.subtitle)),
				ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(c -> Optional.ofNullable(c.icon)),
				Codec.INT.optionalFieldOf("minimum_participants", 1).forGetter(c -> c.minimumParticipants),
				Codec.INT.optionalFieldOf("maximum_participants", 100).forGetter(c -> c.maximumParticipants),
				ResourceLocation.CODEC.optionalFieldOf("intro_slideshow").forGetter(c -> Optional.ofNullable(c.introSlideshow)),
				GamePhaseConfig.CODEC.optionalFieldOf("waiting").forGetter(c -> Optional.ofNullable(c.waiting)),
				Codec.BOOL.optionalFieldOf("is_multi_game").forGetter(c -> Optional.of(c.isMultiGame)),
				GamePhaseConfig.MAP_CODEC.forGetter(c -> c.playing),
				Codec.BOOL.optionalFieldOf("hide_from_list", false).forGetter(c -> c.hideFromList)
		).apply(i, (backendIdOpt, statisticsKeyOpt, name, subtitleOpt, iconOpt, minimumParticipants, maximumParticipants, introSlideshowOpt, waitingOpt, is_multi_game, active, hideFromList) -> {
			ResourceLocation backendId = backendIdOpt.orElse(id);
			String statisticsKey = statisticsKeyOpt.orElse(id.getPath());
			Component subtitle = subtitleOpt.orElse(null);
			boolean isMultiGame = is_multi_game.orElse(false);
			ResourceLocation icon = iconOpt.orElse(null);
			ResourceLocation introSlideshow = introSlideshowOpt.orElse(null);
			GamePhaseConfig waiting = waitingOpt.orElse(null);
			return new GameConfig(id, backendId, statisticsKey, name, subtitle, icon, minimumParticipants, maximumParticipants, introSlideshow, waiting, active, isMultiGame, hideFromList);
		}));
	}

	@Override
	public int getMinimumParticipantCount() {
		return minimumParticipants;
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

	@Override
	public boolean isMultiGamePhase() {
		return isMultiGame;
	}
}
