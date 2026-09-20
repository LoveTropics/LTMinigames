package org.lovetropics.games.common.core.game.config;

import org.lovetropics.games.common.core.game.IGameDefinition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/// Stores data-driven info about a minigame
public record GameConfig(
		Identifier id,
		Component name,
		@Nullable Component subtitle,
		@Nullable Identifier icon,
		int maximumParticipants,
		@Nullable Identifier introSlideshow,
		@Nullable GamePhaseConfig waiting,
		GamePhaseConfig playing,
		boolean hideFromList
) implements IGameDefinition {
	public static Codec<GameConfig> codec(Identifier id) {
		return RecordCodecBuilder.create(i -> i.group(
				ComponentSerialization.CODEC.fieldOf("name").forGetter(c -> c.name),
				ComponentSerialization.CODEC.optionalFieldOf("subtitle").forGetter(c -> Optional.ofNullable(c.subtitle)),
				Identifier.CODEC.optionalFieldOf("icon").forGetter(c -> Optional.ofNullable(c.icon)),
				Codec.INT.optionalFieldOf("maximum_participants", 100).forGetter(c -> c.maximumParticipants),
				Identifier.CODEC.optionalFieldOf("intro_slideshow").forGetter(c -> Optional.ofNullable(c.introSlideshow)),
				GamePhaseConfig.CODEC.optionalFieldOf("waiting").forGetter(c -> Optional.ofNullable(c.waiting)),
				GamePhaseConfig.MAP_CODEC.forGetter(c -> c.playing),
				Codec.BOOL.optionalFieldOf("hide_from_list", false).forGetter(c -> c.hideFromList)
		).apply(i, (name, subtitleOpt, iconOpt, maximumParticipants, introSlideshowOpt, waitingOpt, active, hideFromList) -> {
			Component subtitle = subtitleOpt.orElse(null);
			Identifier icon = iconOpt.orElse(null);
			Identifier introSlideshow = introSlideshowOpt.orElse(null);
			GamePhaseConfig waiting = waitingOpt.orElse(null);
			return new GameConfig(id, name, subtitle, icon, maximumParticipants, introSlideshow, waiting, active, hideFromList);
		}));
	}
}
