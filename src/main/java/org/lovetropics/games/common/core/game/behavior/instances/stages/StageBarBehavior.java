package org.lovetropics.games.common.core.game.behavior.instances.stages;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.util.GameBossBar;
import org.lovetropics.games.common.core.game.util.GameWidgets;
import org.lovetropics.games.common.core.game.util.TemplatedText;
import org.lovetropics.games.common.util.Util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// TODO: Generalise?
public record StageBarBehavior(
		Map<String, BarConfig> barByStage
) implements IGameBehavior {
	public static final MapCodec<StageBarBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(Codec.STRING, BarConfig.CODEC).fieldOf("bar_by_stage").forGetter(StageBarBehavior::barByStage)
	).apply(i, StageBarBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameWidgets widgets = GameWidgets.getOrRegister(game, events);
		State state = new State(
				game.instanceState().getOrThrow(NamedStagesBehaviour.KEY)
		);

		registerGlobalEvents(state, game, events);
		AtomicBoolean isPaused = new AtomicBoolean(false);
		AtomicLong pausedTime = new AtomicLong(0);

		events.listen(GamePhaseEvents.TICK, () -> {
			NamedStagesBehaviour.StageState stageState = state.stages.getCurrentStageState();
			if(stageState != null && stageState.paused() && !isPaused.get()){
				isPaused.set(true);
				pausedTime.set(game.ticks());
			} else if(stageState != null && !stageState.paused() && isPaused.get()){
				isPaused.set(false);
				pausedTime.set(0);
			}
			BarConfig currentBar = stageState != null ? barByStage.get(stageState.id()) : null;
			if (state.override != null) {
				if (currentBar != null) {
					currentBar = currentBar.withTitle(state.override);
				} else {
					currentBar = new BarConfig(state.override, BossEvent.BossBarColor.RED, true);
				}
			}

			if (state.bar != null && !state.bar.config.equals(currentBar)) {
				state.bar.close();
				state.bar = null;
			}
			if (currentBar != null) {
				if (state.bar == null) {
					state.bar = new Bar(widgets, currentBar);
					state.bar.inner.setPlayers(game.allPlayers(true));
				}
				state.bar.tick(isPaused.get() ? pausedTime.get() : game.ticks(), stageState);
			}
		});

		events.listen(SubGameEvents.CREATE, (subGame, subEvents) ->
				registerGlobalEvents(state, subGame, subEvents)
		);
	}

	private static void registerGlobalEvents(State state, IGamePhase game, EventRegistrar events) {
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerGlobalCommands(state, commands, buildContext)
		);
		events.listen(GamePlayerEvents.ADD, player -> {
			if (state.bar != null) {
				state.bar.inner.addPlayer(player);
			}
		});
		events.listen(GamePlayerEvents.REMOVE, player -> {
			if (state.bar != null) {
				state.bar.inner.removePlayer(player);
			}
		});
	}

	private static void registerGlobalCommands(State state, GameCommandRegistrar commands, CommandBuildContext buildContext) {
		commands.register(Commands.literal("bar").then(Commands.literal("override")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("set")
						.then(Commands.argument("text", ComponentArgument.textComponent(buildContext))
								.executes(context -> {
									state.override = new TemplatedText(ComponentArgument.getRawComponent(context, "text"));
									context.getSource().sendSuccess(() -> Component.literal("Added bar override"), true);
									return 1;
								})
						)
				)
				.then(Commands.literal("clear")
						.executes(context -> {
							state.override = null;
							context.getSource().sendSuccess(() -> Component.literal("Cleared bar override"), true);
							return 1;
						})
				)
		));
	}

	private static class State {
		private final NamedStagesBehaviour.State stages;
		private @Nullable Bar bar;
		private @Nullable TemplatedText override;

		private State(NamedStagesBehaviour.State stages) {
			this.stages = stages;
		}
	}

	private static class Bar implements AutoCloseable {
		private final BarConfig config;
		private final GameBossBar inner;

		private Bar(GameWidgets widgets, BarConfig config) {
			this.config = config;
			inner = widgets.openBossBar(CommonComponents.EMPTY, config.color, BossEvent.BossBarOverlay.PROGRESS);
		}

		public void tick(long gameTime, NamedStagesBehaviour.StageState state) {
			if (state.stage().timerLengthTicks().isPresent()) {
				float progress = state.elapsedTicks() / (float) (state.stage().timerLengthTicks().get());
				progress = Mth.clamp(progress, 0.0f, 1.0f);
				float rawProgress = progress;
				if (config.reversed()) {
					progress = 1.0f - progress;
				}
				inner.setProgress(progress);
				long lengthInTicks = state.stage().timerLengthTicks().orElse(0);
				long secondsLeft = (long) ((1.0f - rawProgress) * lengthInTicks) / SharedConstants.TICKS_PER_SECOND;
				Component timeText = Component.literal(Util.formatTimer(secondsLeft)).withStyle(ChatFormatting.AQUA);
				inner.setTitle(config.title.apply(Map.of("time", timeText)));
			} else {
				inner.setProgress(1.0f);
				// Just in case an override has it, pass something for time
				inner.setTitle(config.title.apply(Map.of("time", Component.literal("???"))));
			}
		}

		@Override
		public void close() {
			inner.close();
		}
	}

	private record BarConfig(
			TemplatedText title,
			BossEvent.BossBarColor color,
			boolean reversed
	) {
		public static final Codec<BarConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				TemplatedText.CODEC.fieldOf("title").forGetter(BarConfig::title),
				BossEvent.BossBarColor.CODEC.fieldOf("color").forGetter(BarConfig::color),
				Codec.BOOL.optionalFieldOf("reversed", false).forGetter(BarConfig::reversed)
		).apply(i, BarConfig::new));

		public BarConfig withTitle(TemplatedText title) {
			return new BarConfig(title, color, reversed);
		}
	}
}
