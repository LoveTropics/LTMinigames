package com.lovetropics.minigames.common.core.game.state.statistics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.lovetropics.minigames.common.core.game.state.statistics.StatisticDisplays.*;

public final class StatisticKey<T> {
	private static final CodecRegistry<String, StatisticKey<?>> REGISTRY = CodecRegistry.stringKeys();
	public static final Codec<StatisticKey<?>> CODEC = REGISTRY;
	public static final Codec<StatisticKey<Integer>> INT_CODEC = typedCodec(Integer.class);

	@SuppressWarnings("unchecked")
	public static <T> Codec<StatisticKey<T>> typedCodec(final Class<T> type) {
		return CODEC.comapFlatMap(
				key -> type.isAssignableFrom(key.type) ? DataResult.success((StatisticKey<T>) key) : DataResult.error(() -> "Statistic not of type: " + type),
				key -> key
		);
	}

	// Generic - Per Player
	public static final StatisticKey<Integer> PLACEMENT = ofInt("placement").displays(placement());

	public static final StatisticKey<Integer> KILLS = ofInt("kills").displays(unit("kills"));
	public static final StatisticKey<Integer> POINTS = ofInt("points");

	public static final StatisticKey<Integer> VICTORY_POINTS = ofInt("victory_points").displays(unit("victory points"));
	public static final StatisticKey<Integer> BREAK_BUCKS = ofInt("break_bucks").displays(unit("break bucks"));

	public static final StatisticKey<Integer> TIME_SURVIVED = ofInt("time_survived").displays(minutesSeconds());
	public static final StatisticKey<Integer> ROUNDS_SURVIVED = ofInt("rounds_survived").displays(unit("rounds"));
	public static final StatisticKey<CauseOfDeath> CAUSE_OF_DEATH = register(CauseOfDeath.class, "cause_of_death", CauseOfDeath::serialize);
	public static final StatisticKey<PlayerKey> KILLED_BY = ofPlayer("killed_by");
	public static final StatisticKey<Integer> TIME_CAMPING = ofInt("time_camping").displays(minutesSeconds());
	public static final StatisticKey<GameTeamKey> TEAM = ofTeam("team");

	public static final StatisticKey<Integer> BLOCKS_BROKEN = ofInt("blocks_broken").displays(unit("blocks"));

	public static final StatisticKey<Float> DAMAGE_TAKEN = ofFloat("damage_taken").displays(unit("damage"));
	public static final StatisticKey<Float> DAMAGE_DEALT = ofFloat("damage_dealt").displays(unit("damage"));

	public static final StatisticKey<Integer> DEATHS = ofInt("deaths").displays(unit("deaths"));
	public static final StatisticKey<Boolean> DEAD = ofBool("dead");

	public static final StatisticKey<Integer> ITEMS_CRAFTED = ofInt("items_crafted").displays(unit("items"));

	// Generic - Global
	public static final StatisticKey<Integer> TOTAL_TIME = ofIntNoDefault("total_time").displays(minutesSeconds());

	public static final StatisticKey<Boolean> TEAMS = ofBool("teams");

	public static final StatisticKey<PlayerKey> WINNING_PLAYER = ofPlayer("winning_player").displays(playerName());
	public static final StatisticKey<GameTeamKey> WINNING_TEAM = ofTeam("winning_team");

	public static final StatisticKey<String> MAP = ofString("map");

	public static final StatisticKey<Integer> CRABS = ofInt("crabs").defaultValue(1);

	// Turtle Race
	public static final StatisticKey<Integer> PLAYER_COLLISIONS = ofInt("player_collisions").displays(unit("collisions"));

	// Trash Dive
	public static final StatisticKey<Integer> TRASH_COLLECTED = ofInt("trash_collected").displays(unit("trash"));

	// Signature Run
	public static final StatisticKey<Integer> SIGNATURES_COLLECTED = ofInt("signatures_collected").displays(unit("signatures"));

	// Treasure Dig X
	public static final StatisticKey<Integer> CHESTS_OPENED = ofInt("chests_opened");
	public static final StatisticKey<Integer> EXPLOSIONS_CAUSED = ofInt("explosions_caused");

	private final Class<T> type;
	private final String key;
	private final Function<T, JsonElement> serializer;
	private Function<T, String> display = simple();
	@Nullable
	private T defaultValue;

	private StatisticKey(Class<T> type, String key, Function<T, JsonElement> serializer) {
		this.type = type;
		this.key = key;
		this.serializer = serializer;
	}

	public static <T> StatisticKey<T> register(Class<T> type, String key, Function<T, JsonElement> serializer) {
		StatisticKey<T> statistic = new StatisticKey<>(type, key, serializer);
		REGISTRY.register(key, statistic);
		return statistic;
	}

	public static StatisticKey<Integer> ofInt(String key) {
		return StatisticKey.register(Integer.class, key, JsonPrimitive::new).defaultValue(0);
	}

	public static StatisticKey<Integer> ofIntNoDefault(String key) {
		return StatisticKey.register(Integer.class, key, JsonPrimitive::new);
	}

	public static StatisticKey<Float> ofFloat(String key) {
		return StatisticKey.register(Float.class, key, JsonPrimitive::new).defaultValue(0.0f);
	}

	public static StatisticKey<Boolean> ofBool(String key) {
		return StatisticKey.register(Boolean.class, key, JsonPrimitive::new).defaultValue(false);
	}

	public static StatisticKey<String> ofString(String key) {
		return StatisticKey.register(String.class, key, JsonPrimitive::new);
	}

	public static StatisticKey<PlayerKey> ofPlayer(String key) {
		return StatisticKey.register(PlayerKey.class, key, PlayerKey::serializeId);
	}

	public static StatisticKey<GameTeamKey> ofTeam(String key) {
		return StatisticKey.register(GameTeamKey.class, key, team -> new JsonPrimitive(team.id()));
	}

	public static StatisticKey<IntList> ofIntList(String key) {
		return StatisticKey.intoArray(IntList.class, key, (values, array) -> {
			IntListIterator iterator = values.iterator();
			while (iterator.hasNext()) {
				array.add(new JsonPrimitive(iterator.nextInt()));
			}
		});
	}

	public static StatisticKey<List<String>> ofStringList(String key) {
		return StatisticKey.ofList(key, JsonPrimitive::new);
	}

	@SuppressWarnings("unchecked")
	public static <T> StatisticKey<List<T>> ofList(String key, Function<T, JsonElement> serializeElement) {
		return StatisticKey.intoArray((Class<List<T>>) (Class<?>) List.class, key, (values, array) -> {
			for (T value : values) {
				array.add(serializeElement.apply(value));
			}
		});
	}

	private static <T> StatisticKey<T> intoArray(Class<T> type, String key, BiConsumer<T, JsonArray> serialize) {
		return StatisticKey.register(type, key, values -> {
			JsonArray array = new JsonArray();
			serialize.accept(values, array);
			return array;
		});
	}

	public StatisticKey<T> displays(Function<T, String> display) {
		this.display = display;
		return this;
	}

	public StatisticKey<T> defaultValue(T defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	public String getKey() {
		return key;
	}

	@Nullable
	public T defaultValue() {
		return defaultValue;
	}

	public JsonElement serialize(T value) {
		return serializer.apply(value);
	}

	@SuppressWarnings("unchecked")
	public JsonElement serializeUnchecked(Object value) {
		return serialize((T) value);
	}

	public String display(T value) {
		return display.apply(value);
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public String toString() {
		return "StatisticKey(" + key + ")";
	}

	@Nullable
	public static StatisticKey<?> get(String key) {
		return REGISTRY.get(key);
	}
}
