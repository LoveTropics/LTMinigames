package com.lovetropics.minigames.common.core.game.datagen;

import com.google.common.base.Suppliers;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorTemplate;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GamePhaseConfig;
import com.lovetropics.minigames.common.core.game.map.IGameMapProvider;
import com.lovetropics.minigames.common.util.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

public class GameBuilder {
	private final ResourceLocation id;
	@Nullable
	private ResourceLocation backendId;
	@Nullable
	private String statisticsKey;
	@Nullable
	private Component name;
	@Nullable
	private Component subtitle;
	@Nullable
	private ResourceLocation icon;
	private int minimumParticipants = 1;
	private int maximumParticipants = 50;
	@Nullable
	private GamePhaseConfig waiting;
	@Nullable
	private GamePhaseConfig playing;
	private boolean hasMultiGame = false;
	private boolean hideFromList;

	public GameBuilder(ResourceLocation id) {
		this.id = id;
		backendId = id;
		statisticsKey = id.getPath();
		name = Component.literal(id.toString());
	}

	public GameBuilder setBackendId(ResourceLocation backendId) {
		this.backendId = backendId;
		return this;
	}

	public GameBuilder setStatisticsKey(String statisticsKey) {
		this.statisticsKey = statisticsKey;
		return this;
	}

	public GameBuilder setName(Component name) {
		this.name = name;
		return this;
	}

	public GameBuilder setSubtitle(@Nullable Component subtitle) {
		this.subtitle = subtitle;
		return this;
	}

	public GameBuilder setIcon(@Nullable ResourceLocation icon) {
		this.icon = icon;
		return this;
	}

	public GameBuilder setMinimumParticipants(int minimumParticipants) {
		this.minimumParticipants = minimumParticipants;
		return this;
	}

	public GameBuilder setMaximumParticipants(int maximumParticipants) {
		this.maximumParticipants = maximumParticipants;
		return this;
	}

	public GameBuilder withWaitingPhase(IGameMapProvider map, UnaryOperator<PhaseBuilder> builderConsumer) {
		waiting = builderConsumer.apply(new PhaseBuilder(map)).create();
		return this;
	}

	public GameBuilder withPlayingPhase(IGameMapProvider map, UnaryOperator<PhaseBuilder> builderConsumer) {
		playing = builderConsumer.apply(new PhaseBuilder(map)).create();
		return this;
	}

	public GameBuilder setMultiGame(boolean multiGame) {
		this.hasMultiGame = multiGame;
		return this;
	}

	public GameBuilder setHideFromList(boolean hideFromList) {
		this.hideFromList = hideFromList;
		return this;
	}

	public GameConfig build() {
		Objects.requireNonNull(playing, "Playing phase must be initialized");
		return new GameConfig(id, backendId, statisticsKey, name, subtitle, icon, minimumParticipants, maximumParticipants, waiting, playing, hasMultiGame, hideFromList);
	}

	public static final class PhaseBuilder {
		private final IGameMapProvider map;
		@Nullable
		private AABB area;
		private final List<BehaviorTemplate> behaviors = new ArrayList<>();

		public PhaseBuilder(IGameMapProvider map) {
			this.map = map;
		}

		public PhaseBuilder setArea(@Nullable AABB area) {
			this.area = area;
			return this;
		}

		public PhaseBuilder withBehavior(IGameBehavior... behavior) {
			for (IGameBehavior b : behavior) {
				behaviors.add(new BehaviorTemplate.Direct(Suppliers.ofInstance(b)));
			}
			return this;
		}

		public GamePhaseConfig create() {
			return new GamePhaseConfig(map, area == null ? Util.INFINITE_AABB : area, behaviors);
		}
	}
}
