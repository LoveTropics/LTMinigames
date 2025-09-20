package com.lovetropics.minigames.common.core.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.ServerLevelData;

public final class MapWorldSettings {
	private static final Codec<GameRules> GAME_RULES_CODEC = Codec.PASSTHROUGH.xmap(
			dynamic -> {
				GameRules gameRules = new GameRules(FeatureFlagSet.of());
				gameRules.loadFromTag(dynamic);
				return gameRules;
			},
			gameRules -> new Dynamic<>(NbtOps.INSTANCE, gameRules.createTag())
	);

	public static final Codec<MapWorldSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
			GAME_RULES_CODEC.fieldOf("game_rules").forGetter(s -> s.gameRules),
			Codec.LONG.fieldOf("time_of_day").forGetter(s -> s.timeOfDay),
			Codec.INT.fieldOf("sunny_time").forGetter(s -> s.sunnyTime),
			Codec.BOOL.fieldOf("raining").forGetter(s -> s.raining),
			Codec.INT.fieldOf("rain_time").forGetter(s -> s.rainTime),
			Codec.BOOL.fieldOf("thundering").forGetter(s -> s.thundering),
			Codec.INT.fieldOf("thunder_time").forGetter(s -> s.thunderTime),
			Difficulty.CODEC.fieldOf("difficulty").forGetter(s -> s.difficulty)
	).apply(i, MapWorldSettings::new));

	public final GameRules gameRules;
	public long timeOfDay;

	public int sunnyTime;
	public boolean raining;
	public int rainTime;
	public boolean thundering;
	public int thunderTime;
	public Difficulty difficulty;

	public MapWorldSettings(GameRules gameRules, long timeOfDay, int sunnyTime, boolean raining, int rainTime, boolean thundering, int thunderTime, Difficulty difficulty) {
		this.gameRules = gameRules;
		this.timeOfDay = timeOfDay;
		this.sunnyTime = sunnyTime;
		this.raining = raining;
		this.rainTime = rainTime;
		this.thundering = thundering;
		this.thunderTime = thunderTime;
		this.difficulty = difficulty;
	}

	public MapWorldSettings() {
		this(new GameRules(FeatureFlagSet.of()), 0, 0, false, 0, false, 0, Difficulty.NORMAL);
	}

	public static MapWorldSettings createFromOverworld(MinecraftServer server) {
		return createFrom((ServerLevelData) server.overworld().getLevelData());
	}

	public static MapWorldSettings createFrom(ServerLevelData info) {
		return new MapWorldSettings(
				info.getGameRules().copy(FeatureFlagSet.of()),
				info.getDayTime(),
				info.getClearWeatherTime(),
				info.isRaining(),
				info.getRainTime(),
				info.isThundering(),
				info.getThunderTime(),
				info.getDifficulty()
		);
	}

	public void importFrom(MapWorldSettings settings) {
		settings.gameRules.loadFromTag(new Dynamic<>(NbtOps.INSTANCE, settings.gameRules.createTag()));
		timeOfDay = settings.timeOfDay;
		sunnyTime = settings.sunnyTime;
		raining = settings.raining;
		rainTime = settings.rainTime;
		thundering = settings.thundering;
		thunderTime = settings.thunderTime;
		difficulty = settings.difficulty;
	}
}
