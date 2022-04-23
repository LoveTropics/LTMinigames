package com.lovetropics.minigames.common.core.game.weather;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

public final class VanillaWeatherController implements WeatherController {
	private final ServerWorld world;
	private final WeatherState state = new WeatherState();

	public VanillaWeatherController(ServerWorld world) {
		this.world = world;
	}

	@Override
	public void onPlayerJoin(ServerPlayerEntity player) {
	}

	@Override
	public void tick() {
		world.getLevelData().setRaining(state.isRaining());
	}

	@Override
	public void setRain(float amount, RainType type) {
		state.rainAmount = amount;
		state.rainType = type;
	}

	@Override
	public void setWind(float speed) {
		state.windSpeed = speed;
	}

	@Override
	public void setHeatwave(boolean heatwave) {
		state.heatwave = heatwave;
	}

	@Override
	public void setSandstorm(int buildupTickRate, int maxStackable) {
		state.sandstorm = new StormState(buildupTickRate, maxStackable);
	}

	@Override
	public void clearSandstorm() {
		state.sandstorm = null;
	}

	@Override
	public void setSnowstorm(int buildupTickRate, int maxStackable) {
		state.snowstorm = new StormState(buildupTickRate, maxStackable);
	}

	@Override
	public void clearSnowstorm() {
		state.snowstorm = null;
	}

	@Override
	public float getRainAmount() {
		return state.rainAmount;
	}

	@Override
	public RainType getRainType() {
		return state.rainType;
	}

	@Override
	public float getWindSpeed() {
		return state.windSpeed;
	}

	@Override
	public boolean isHeatwave() {
		return state.heatwave;
	}

	@Nullable
	@Override
	public StormState getSandstorm() {
		return state.sandstorm;
	}

	@Nullable
	@Override
	public StormState getSnowstorm() {
		return state.snowstorm;
	}
}
