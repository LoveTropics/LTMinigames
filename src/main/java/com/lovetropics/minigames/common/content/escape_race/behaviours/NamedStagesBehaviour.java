package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.IGameState;
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
import org.slf4j.Logger;

import javax.annotation.Nullable;
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
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			commands.register(Commands.literal("stage")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
		});
	}

	public static final class State implements IGameState {

		private final IGamePhase game;
		private final NamedStagesBehaviour parent;

		private long currentStageStartTime = -1;
		private long currentStageEndTime = -1;
		private long pausedAt = -1;
		private boolean running = false;
		private @Nullable NamedStage currentStage;
		private @Nullable String currentStageName;

		private State(NamedStagesBehaviour parent, IGamePhase game) {
			this.parent = parent;
			this.game = game;
		}

		public void pause(){
			if(currentStage != null && running){
				running = false;
				pausedAt = game.ticks();
			}
		}

		public void start(){
			if(currentStage != null && !running){
				running = true;
				if(currentStageStartTime != -1 && currentStage.timerLengthTicks.isPresent()){
					long ticksThrough = pausedAt - currentStageStartTime;
					long ticksRemaining = currentStage.timerLengthTicks.get() - ticksThrough;
					currentStageEndTime = game.ticks() + ticksRemaining;
				}
				pausedAt = -1;
			}
		}

		public void tick(){
			if(running) {
				if (currentStage != null) {
					if(currentStage.timerLengthTicks.isPresent()) {
						long currentTick = currentStage.timerLengthTicks.get() - (currentStageEndTime - game.ticks());
						GameActionList actions = currentStage.tickActions.get(currentTick);
						if (actions != null) {
							actions.apply(game, ContextMap.EMPTY);
						}
						if (currentStage.autoMoveOn() && currentStage.nextStage().isPresent()) {
							if (game.ticks() >= currentStageEndTime) {
								progressToStage(currentStage.nextStage().get());
							}
						}
					}
				}
			}
		}

		public void progressToNext(boolean skipEndActions){
			if(currentStage != null && currentStage.nextStage().isPresent()){
				String stage = currentStage.nextStage().get();
				progressToStage(stage, skipEndActions);
			}
		}

		public void progressToNext(){
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
			if(currentStage != null && !skipEndActions){
				currentStage.endActions.ifPresent(actions -> actions.apply(game, ContextMap.EMPTY));
				if(currentStage.endActions().isPresent() && !currentStage.endAutoProgress) {
					return;
				}
			}
			currentStage = stage;
			currentStageName = stageName;
			currentStageStartTime = game.ticks();
			if(currentStage.timerLengthTicks().isPresent()) {
				currentStageEndTime = game.ticks() + currentStage.timerLengthTicks().get();
			} else {
				currentStageEndTime = -1;
			}
			currentStage.startActions.ifPresent(actions -> actions.apply(game, ContextMap.EMPTY));
		}

		public @Nullable StageState getCurrentStageState() {
			if (currentStage == null | currentStageName == null) {
				return null;
			}
			return new StageState(
					currentStageName,
					currentStage,
					currentStageStartTime,
					currentStageEndTime
			);
		}
	}

	public record StageState(
			String id,
			NamedStage stage,
			long startTime,
			long endTime
	) {
	}
}
