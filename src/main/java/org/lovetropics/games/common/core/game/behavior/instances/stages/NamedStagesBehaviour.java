package org.lovetropics.games.common.core.game.behavior.instances.stages;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

public record NamedStagesBehaviour(
		Map<String, NamedStage> stages,
		String startingStage,
		boolean autoStart
) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<NamedStagesBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(Codec.STRING, NamedStage.CODEC).fieldOf("stages").forGetter(NamedStagesBehaviour::stages),
			Codec.STRING.fieldOf("starting_stage").forGetter(NamedStagesBehaviour::startingStage),
			Codec.BOOL.optionalFieldOf("auto_start", true).forGetter(NamedStagesBehaviour::autoStart)
	).apply(i, NamedStagesBehaviour::new));

	public static final GameStateKey<NamedStagesBehaviour.State> KEY = GameStateKey.create("stages");

	public record NamedStage(
			Optional<Integer> timerLengthTicks,
			Optional<GameActionList> startActions,
			Optional<GameActionList> endActions,
			Optional<String> nextStage,
			boolean autoMoveOn,
			Long2ObjectMap<GameActionList> tickActions,
			boolean endAutoProgress
	) {
		public static final Codec<NamedStage> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.optionalFieldOf("timer_length_ticks").forGetter(NamedStage::timerLengthTicks),
				GameActionList.CODEC.optionalFieldOf("start_actions").forGetter(NamedStage::startActions),
				GameActionList.CODEC.optionalFieldOf("end_actions").forGetter(NamedStage::endActions),
				Codec.STRING.optionalFieldOf("next_stage").forGetter(NamedStage::nextStage),
				Codec.BOOL.optionalFieldOf("auto_move_on", false).forGetter(NamedStage::autoMoveOn),
				MoreCodecs.long2Object(GameActionList.CODEC).optionalFieldOf("tick_actions", Long2ObjectMaps.emptyMap()).forGetter(NamedStage::tickActions),
				Codec.BOOL.optionalFieldOf("end_auto_progress", true).forGetter(NamedStage::endAutoProgress)
		).apply(i, NamedStage::new));

	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		State state = new State(this, game);
		game.instanceState().register(KEY, state);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		State state = game.instanceState().getOrThrow(KEY);
		stages.values().forEach(stage -> {
			stage.startActions().ifPresent(actions -> {actions.register(game, events);});
			stage.endActions().ifPresent(actions -> {actions.register(game, events);});
			stage.tickActions().forEach((s, gameActionList) -> {
				gameActionList.register(game, events);
			});
		});
		events.listen(GamePhaseEvents.START, (initiator) -> {
			state.progressToStage(startingStage);
			if(autoStart) {
				state.start();
			}
		});
		events.listen(GamePhaseEvents.TICK, state::tick);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerCommands(commands, state)
		);
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			subEvents.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
					registerCommands(commands, state)
			);
		});
	}

	private void registerCommands(GameCommandRegistrar commands, State state) {
		commands.register(Commands.literal("stage")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("speed")
						.then(Commands.argument("speed", IntegerArgumentType.integer(1, 100))
						.executes(context -> {
							state.setSpeedModifier(IntegerArgumentType.getInteger(context, "speed"));
							context.getSource().sendSuccess(() -> Component.literal("Speed updated.").withStyle(ChatFormatting.GREEN), true);
							return 1;
						})))
				.then(Commands.literal("pause")
						.executes(context -> {
							if(state.running){
								state.pause();
								context.getSource().sendSuccess(() -> Component.literal("Current stage paused.").withStyle(ChatFormatting.GREEN), true);
							} else {
								context.getSource().sendFailure(Component.literal("Stage is not currently running.").withStyle(ChatFormatting.RED));
							}
							return 1;
						}))
				.then(Commands.literal("resume")
						.executes(context -> {
							if(!state.running){
								state.start();
								context.getSource().sendSuccess(() -> Component.literal("Current stage resumed.").withStyle(ChatFormatting.GREEN), true);
							} else {
								context.getSource().sendFailure(Component.literal("Stage is currently running.").withStyle(ChatFormatting.RED));
							}
							return 1;
						}))
				.then(Commands.literal("next")
						.executes(context -> {
							if(state.currentStage != null){
								if(state.currentStage.nextStage().isPresent()){
									String stage = state.currentStage.nextStage().get();
									context.getSource().sendSuccess(() -> Component.translatable("Progressing on to %s", stage).withStyle(ChatFormatting.GREEN), true);
									state.progressToStage(stage);
								} else {
									context.getSource().sendFailure(Component.literal("No stage to move on to.").withStyle(ChatFormatting.RED));
								}
							} else {
								context.getSource().sendFailure(Component.literal("No current stage.").withStyle(ChatFormatting.RED));
							}
							return 1;
						})
				)
				.then(Commands.literal("skip")
						.then(Commands.argument("stage", StringArgumentType.string())
								.suggests((context, builder) ->
										SharedSuggestionProvider.suggest(stages.keySet().stream(), builder)
								)
								.executes(context -> {
									String stageToSkipTo = StringArgumentType.getString(context, "stage");
									if(!stages.containsKey(stageToSkipTo)) {
										context.getSource().sendFailure(Component.literal("No stage exists with that name").withStyle(ChatFormatting.RED));
										return 0;
									}
									state.progressToStage(stageToSkipTo, true);
									context.getSource().sendSuccess(() -> Component.translatable("Skipping on to %s", stageToSkipTo).withStyle(ChatFormatting.GREEN), true);
									return 1;
								})
						))
		);
	}

    public static final class State implements IGameState {

        private final IGamePhase game;
        private final NamedStagesBehaviour parent;

		private long stageTicksElapsed = 0;
		private int speedModifier = 1;
        private boolean running = false;
        private @Nullable NamedStage currentStage;
        private @Nullable String currentStageName;

        private State(NamedStagesBehaviour parent, IGamePhase game) {
            this.parent = parent;
            this.game = game;
        }

		public void setSpeedModifier(int speedModifier) {
			this.speedModifier = speedModifier;
		}

        public void pause() {
            if (currentStage != null && running) {
                running = false;
            }
        }

        public void start() {
            if (currentStage != null && !running) {
                running = true;
            }
        }

        public void tick() {
			NamedStage stage = currentStage;
			if(!running || stage == null || stage.timerLengthTicks().isEmpty()){
				return;
			}
			long length = stage.timerLengthTicks().get();
			long from = stageTicksElapsed;
			long to = from + speedModifier;

			for(long t = from + 1; t <= to; t++){
				stageTicksElapsed = t;
				GameActionList actions = stage.tickActions.get(t);
				if (actions != null) {
					actions.apply(game, ContextMap.EMPTY);
				}
				if(currentStage != stage || !running){
					return;
				}
			}
			if (stage.autoMoveOn() && stage.nextStage().isPresent() && stageTicksElapsed >= length) {
				progressToStage(stage.nextStage().get());
			}
        }

        public void progressToNext(boolean skipEndActions) {
            if (currentStage != null && currentStage.nextStage().isPresent()) {
                progressToStage(currentStage.nextStage.get(), skipEndActions);
            }
        }

        public void progressToNext() {
            progressToNext(false);
        }

        public void progressToStage(String stage) {
            progressToStage(stage, false);
        }

        public void progressToStage(String stageName, boolean skipEndActions) {
            NamedStage stage = parent.stages().get(stageName);
            if (stage == null) {
                LOGGER.warn("No stage with name: {}", stageName);
                return;
            }
            if (currentStage != null && !skipEndActions) {
                currentStage.endActions.ifPresent(actions -> actions.apply(game, ContextMap.EMPTY));
                if (currentStage.endActions().isPresent() && !currentStage.endAutoProgress) {
                    return;
                }
            }
            currentStage = stage;
            currentStageName = stageName;
			stageTicksElapsed = 0;
            currentStage.startActions.ifPresent(actions -> actions.apply(game, ContextMap.EMPTY));
        }

        public @Nullable StageState getCurrentStageState() {
            if (currentStage == null | currentStageName == null) {
                return null;
            }
            return new StageState(
                    currentStageName,
                    currentStage,
					stageTicksElapsed,
					!running
            );
        }
    }

	public record StageState(
			String id,
			NamedStage stage,
			long elapsedTicks,
			boolean paused
	) {
	}
}
