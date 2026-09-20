package org.lovetropics.games.common.core.game.weather;

import com.lovetropics.lib.codec.MoreCodecs;
import org.lovetropics.games.common.content.MinigameTexts;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

public enum WeatherEventType {
	HEAVY_RAIN("heavy_rain", MinigameTexts.HEAVY_RAIN),
	ACID_RAIN("acid_rain", MinigameTexts.ACID_RAIN),
	HAIL("hail", MinigameTexts.HAIL),
	HEATWAVE("heatwave", MinigameTexts.HEATWAVE),
	SANDSTORM("sandstorm", MinigameTexts.SANDSTORM),
	SNOWSTORM("snowstorm", MinigameTexts.SNOWSTORM);

	public static final Codec<WeatherEventType> CODEC = MoreCodecs.stringVariants(values(), WeatherEventType::getKey);

	private final String key;
	private final Component name;

	WeatherEventType(String key, Component name) {
		this.key = key;
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public Component getName() {
		return name;
	}
}
