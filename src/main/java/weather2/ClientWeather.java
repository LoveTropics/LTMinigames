package weather2;

import com.lovetropics.minigames.common.core.game.weather.RainType;
import com.lovetropics.minigames.common.core.game.weather.WeatherState;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import weather2.client.SceneEnhancer;

@Mod.EventBusSubscriber(modid = Weather.MODID, value = Dist.CLIENT)
public final class ClientWeather {
	private static ClientWeather instance = new ClientWeather();

	private static final int LERP_TICKS = ServerWeatherController.UPDATE_INTERVAL;

	private final WeatherState state = new WeatherState();

	private WeatherState lerpState = this.state;
	private int lerpTicks;

	private ClientWeather() {
	}

	public static ClientWeather get() {
		return instance;
	}

	@SubscribeEvent
	public static void onWorldLoad(WorldEvent.Load event) {
		if (event.getWorld().isRemote()) {
			reset();
		}
	}

	public static void reset() {
		instance = new ClientWeather();
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			instance.tick();
		}
	}

	public void onUpdateWeather(WeatherState state) {
		this.lerpTicks = LERP_TICKS;
		this.lerpState = state;
	}

	public void tick() {
		if (this.lerpTicks <= 0) {
			this.state.rainType = this.lerpState.rainType;
			this.state.heatwave = this.lerpState.heatwave;
			this.state.sandstorm = this.lerpState.sandstorm;
			this.state.snowstorm = this.lerpState.snowstorm;
			return;
		}

		float lerpTicks = this.lerpTicks--;
		this.state.rainAmount = this.state.rainAmount + (this.lerpState.rainAmount - this.state.rainAmount) / lerpTicks;
		this.state.windSpeed = this.state.windSpeed + (this.lerpState.windSpeed - this.state.windSpeed) / lerpTicks;
	}

	public float getRainAmount() {
		return this.state.rainAmount;
	}

	public float getVanillaRainAmount() {
		//have vanilla rain get to max saturation by the time rainAmount hits 0.33, rest of range is used for extra particle effects elsewhere
		return Math.min(this.state.rainAmount * 3F, 1F);
	}

	public RainType getRainType() {
		return this.state.rainType;
	}

	public float getWindSpeed() {
		return this.state.windSpeed;
	}

	public boolean isHeatwave() {
		if (false) return true;
		if (Minecraft.getInstance().world != null && Minecraft.getInstance().world.getGameTime() % 800 >= 600) return true;
		return this.state.heatwave;
	}

	public boolean isSandstorm() {
		if (false) return true;
		if (Minecraft.getInstance().world != null && Minecraft.getInstance().world.getGameTime() % 800 >= 400 && Minecraft.getInstance().world.getGameTime() % 800 < 600) return true;
		return this.state.sandstorm;
	}

	public boolean isSnowstorm() {
		if (false) return true;
		if (Minecraft.getInstance().world != null && Minecraft.getInstance().world.getGameTime() % 800 >= 200 && Minecraft.getInstance().world.getGameTime() % 800 < 400) return true;
		//if (Minecraft.getInstance().world != null && Minecraft.getInstance().world.getGameTime() % 400 < 200) return true;
		return this.state.snowstorm;
	}

	public boolean hasWeather() {
		if (SceneEnhancer.FORCE_ON_DEBUG_TESTING) return true;
		return this.state.hasWeather();
	}
}
