package org.lovetropics.games.common.core.game.persistent;

import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.behavior.event.GameEventListeners;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import org.lovetropics.games.common.core.game.region.GameRegions;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.maps.MapRegions;

import java.util.function.Function;

public class PersistentGameInstance implements PersistentGame {
	// TODO: Figure out something better than this, as we evolve persistent games
	private static @Nullable Function<ServerLevel, MapRegions> regionsGetter;

	private final MutablePlayerSet players = new MutablePlayerSet();
	private final GameEventListeners events = new GameEventListeners();
	private final ServerLevel level;
	private final GameRegions regions;

	public PersistentGameInstance(ServerLevel level) {
		this.level = level;
		MapRegions mapRegions = regionsGetter != null ? regionsGetter.apply(level) : null;
		if (mapRegions != null) {
			regions = key -> mapRegions.get(key).stream()
					.map(box -> new Region(level, box))
					.toList();
		} else {
			regions = GameRegions.EMPTY;
		}
	}

	public static void setRegionsGetter(@Nullable Function<ServerLevel, MapRegions> regionsGetter) {
		PersistentGameInstance.regionsGetter = regionsGetter;
	}

	@Override
	public GameEventListeners events() {
		return events;
	}

	@Override
	public <T> T invoker(GameEventType<T> type) {
		return events.invoker(type);
	}

	@Override
	public MutablePlayerSet players() {
		return players;
	}

	@Override
	public ServerLevel level() {
		return level;
	}

	@Override
	public GameRegions gameRegions() {
		return regions;
	}
}
