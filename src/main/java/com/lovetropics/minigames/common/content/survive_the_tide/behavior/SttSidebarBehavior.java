package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTideTexts;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.lovetropics.minigames.common.core.game.state.weather.GameWeatherState;
import com.lovetropics.minigames.common.core.game.util.GameSidebar;
import com.lovetropics.minigames.common.core.game.util.GlobalGameWidgets;
import com.lovetropics.minigames.common.core.game.weather.WeatherEventType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

// TODO: make it generic and data-driven
public class SttSidebarBehavior implements IGameBehavior {
	public static final MapCodec<SttSidebarBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressionPeriod.CODEC.fieldOf("safe_period").forGetter(b -> b.safePeriod),
			ProgressionPeriod.CODEC.fieldOf("tide_rising_period").forGetter(b -> b.tideRisingPeriod),
			ProgressionPeriod.CODEC.fieldOf("iceberg_growth_period").forGetter(b -> b.icebergGrowthPeriod),
			ProgressionPeriod.CODEC.fieldOf("explosive_storm_period").forGetter(b -> b.explosiveStormPeriod)
	).apply(i, SttSidebarBehavior::new));

	private final ProgressionPeriod safePeriod;
	private final ProgressionPeriod tideRisingPeriod;
	private final ProgressionPeriod icebergGrowthPeriod;
	private final ProgressionPeriod explosiveStormPeriod;

	private GlobalGameWidgets widgets;
	private GameSidebar sidebar;

	private IGamePhase game;
	private ProgressHolder progression;
	private GameWeatherState weather;

	private int initialPlayerCount;

	private long lastUpdateTime;
	private boolean dirty;

	public SttSidebarBehavior(ProgressionPeriod safePeriod, ProgressionPeriod tideRisingPeriod, ProgressionPeriod icebergGrowthPeriod, ProgressionPeriod explosiveStormPeriod) {
		this.safePeriod = safePeriod;
		this.tideRisingPeriod = tideRisingPeriod;
		this.icebergGrowthPeriod = icebergGrowthPeriod;
		this.explosiveStormPeriod = explosiveStormPeriod;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		widgets = GlobalGameWidgets.registerTo(game, events);

		progression = ProgressChannel.MAIN.getOrThrow(game);
		weather = game.state().getOrThrow(GameWeatherState.KEY);

		events.listen(GamePhaseEvents.START, initiator -> {
			sidebar = widgets.openSidebar(game.definition().name().copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
			initialPlayerCount = game.participants().size();
		});

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> dirty = true);

		events.listen(GamePhaseEvents.TICK, () -> {
			if (dirty || game.ticks() - lastUpdateTime > SharedConstants.TICKS_PER_SECOND) {
				sidebar.set(buildSidebar());
				lastUpdateTime = game.ticks();
				dirty = false;
			}
		});
	}

	private Component[] buildSidebar() {
		return new Component[]{
				SurviveTheTideTexts.SIDEBAR_WEATHER.apply(weatherName()),
				phaseState(),
				CommonComponents.EMPTY,
				playersState()
		};
	}

	private Component weatherName() {
		WeatherEventType type = weather.getEventType();
		return type != null ? type.getName() : MinigameTexts.CLEAR_WEATHER;
	}

	private Component phaseState() {
		if (progression.is(safePeriod)) {
			return SurviveTheTideTexts.SIDEBAR_PVP_DISABLED;
		} else if (progression.is(tideRisingPeriod)) {
			return SurviveTheTideTexts.SIDEBAR_TIDE_RISING;
		} else if (progression.is(icebergGrowthPeriod)) {
			return SurviveTheTideTexts.SIDEBAR_ICEBERGS_FORMING;
		} else if (progression.is(explosiveStormPeriod)) {
			return SurviveTheTideTexts.SIDEBAR_EXPLOSIVE_STORM;
		}
		return CommonComponents.EMPTY;
	}

	private Component playersState() {
		int playerCount = game.participants().size();
		return SurviveTheTideTexts.SIDEBAR_PLAYER_COUNT.apply(playerCount, initialPlayerCount);
	}
}
