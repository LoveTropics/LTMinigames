package com.lovetropics.minigames.common.core.game.behavior.action;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.Supplier;

public record ApplyToBehavior<T, A extends ActionTarget<T>>(A target, GameActionList<T> actions, Supplier<GameBehaviorType<ApplyToBehavior<T, A>>> type) implements IGameBehavior {
	public static final MapCodec<ApplyToBehavior<Plot, PlotActionTarget>> PLOT_CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			PlotActionTarget.CODEC.optionalFieldOf("target", PlotActionTarget.ALL).forGetter(ApplyToBehavior::target),
			GameActionList.codec(ActionTargetTypes.PLOT, new PlotActionTarget(PlotActionTarget.Target.SOURCE)).fieldOf("actions").forGetter(ApplyToBehavior::actions)
	).apply(in, (a, b) -> new ApplyToBehavior<>(a, b, GameBehaviorTypes.APPLY_TO_PLOT)));
	public static final MapCodec<ApplyToBehavior<ServerPlayer, PlayerActionTarget>> PLAYER_CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			PlayerActionTarget.CODEC.optionalFieldOf("target", PlayerActionTarget.ALL).forGetter(ApplyToBehavior::target),
			GameActionList.codec(ActionTargetTypes.PLAYER, PlayerActionTarget.SOURCE).fieldOf("actions").forGetter(ApplyToBehavior::actions)
	).apply(in, (a, b) -> new ApplyToBehavior<>(a, b, GameBehaviorTypes.APPLY_TO_PLAYER)));
	public static final MapCodec<ApplyToBehavior<GameTeam, TeamActionTarget>> TEAM_CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			TeamActionTarget.CODEC.optionalFieldOf("target", TeamActionTarget.ALL).forGetter(ApplyToBehavior::target),
			GameActionList.codec(ActionTargetTypes.TEAM, TeamActionTarget.SOURCE).fieldOf("actions").forGetter(ApplyToBehavior::actions)
	).apply(in, (a, b) -> new ApplyToBehavior<>(a, b, GameBehaviorTypes.APPLY_TO_TEAM)));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);

		// TODO - this is super cursed, but if the target requires a source, this action will be a source transformation action, otherwise it will convert the given actions into a raw APPLY action
		if (target.requiresSource()) {
			target.listenAndCaptureSource(events, (context, source) -> actions.apply(game, context, target.resolve(game, source)));
		} else {
			events.listen(GameActionEvents.APPLY, context -> actions.apply(game, context, target.resolve(game, List.of())));
		}
	}

	@Override
	public Supplier<GameBehaviorType<?>> behaviorType() {
		return type::get;
	}
}
