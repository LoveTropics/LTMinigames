package com.lovetropics.minigames.common.core.game.weather;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class WeatherEvent {
	private final WeatherEventType type;
	private long time;

	@Nullable
	private Consumer<WeatherController> apply;
	@Nullable
	private Consumer<WeatherController> remove;

	private WeatherEvent(WeatherEventType type, long time) {
		this.type = type;
		this.time = time;
	}

	// TODO: System to define weather events in data, including parameters
	@Nullable
	public static WeatherEvent createGeneric(WeatherEventType type, long time) {
		return switch (type) {
			case HEATWAVE -> heatwave(time);
			case ACID_RAIN -> acidRain(time);
			case HEAVY_RAIN -> heavyRain(time);
			case HAIL -> hail(time);
			default -> null;
		};
	}

	public static WeatherEvent heavyRain(long time) {
		return new WeatherEvent(WeatherEventType.HEAVY_RAIN, time)
				.applies(controller -> controller.setRain(1.0F, PrecipitationType.NORMAL))
				.removes(controller -> controller.setRain(0.0F, PrecipitationType.NORMAL));
	}

	public static WeatherEvent acidRain(long time) {
		return new WeatherEvent(WeatherEventType.ACID_RAIN, time)
				.applies(controller -> controller.setRain(1.0F, PrecipitationType.ACID))
				.removes(controller -> controller.setRain(0.0F, PrecipitationType.ACID));
	}

	public static WeatherEvent hail(long time) {
		return new WeatherEvent(WeatherEventType.HAIL, time)
				.applies(controller -> controller.setRain(1.0F, PrecipitationType.HAIL))
				.removes(controller -> controller.setRain(0.0F, PrecipitationType.HAIL));
	}

	public static WeatherEvent heatwave(long time) {
		return new WeatherEvent(WeatherEventType.HEATWAVE, time)
				.applies(controller -> controller.setHeatwave(true))
				.removes(controller -> controller.setHeatwave(false));
	}

	public static WeatherEvent sandstorm(long time, int buildupTickRate, int maxStackable) {
		return new WeatherEvent(WeatherEventType.SANDSTORM, time)
				.applies(controller -> controller.setSandstorm(buildupTickRate, maxStackable))
				.removes(WeatherController::clearSandstorm);
	}

	public static WeatherEvent snowstorm(long time, int buildupTickRate, int maxStackable) {
		return new WeatherEvent(WeatherEventType.SNOWSTORM, time)
				.applies(controller -> controller.setSnowstorm(buildupTickRate, maxStackable))
				.removes(WeatherController::clearSnowstorm);
	}

	public WeatherEvent applies(Consumer<WeatherController> apply) {
		this.apply = apply;
		return this;
	}

	public WeatherEvent removes(Consumer<WeatherController> remove) {
		this.remove = remove;
		return this;
	}

	public WeatherEventType getType() {
		return type;
	}

	public void apply(WeatherController controller) {
		if (apply != null) {
			apply.accept(controller);
		}
	}

	public void remove(WeatherController controller) {
		if (remove != null) {
			remove.accept(controller);
		}
	}

	public TickResult tick() {
		if (time-- <= 0) {
			return TickResult.STOP;
		}
		return TickResult.CONTINUE;
	}

	public enum TickResult {
		STOP,
		CONTINUE,
	}
}
