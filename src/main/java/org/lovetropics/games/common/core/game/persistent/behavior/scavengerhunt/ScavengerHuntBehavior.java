package org.lovetropics.games.common.core.game.persistent.behavior.scavengerhunt;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.data.LoveTropicsAttachments;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentScoreboards;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.util.Util;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public record ScavengerHuntBehavior(
		List<Stage> stages,
		Optional<Action> unlockAction,
		Optional<Action> missingPreviousAction,
		Optional<Action> alreadyUnlockedAction
) implements PersistentGameBehavior {
	public static final MapCodec<ScavengerHuntBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Stage.CODEC.listOf().fieldOf("stages").forGetter(ScavengerHuntBehavior::stages),
			Action.CODEC.optionalFieldOf("on_unlock").forGetter(ScavengerHuntBehavior::unlockAction),
			Action.CODEC.optionalFieldOf("on_missing_previous").forGetter(ScavengerHuntBehavior::missingPreviousAction),
			Action.CODEC.optionalFieldOf("on_already_unlocked").forGetter(ScavengerHuntBehavior::alreadyUnlockedAction)
	).apply(instance, ScavengerHuntBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int NO_STAGE = -1;

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, hitResult) -> {
			int selectedStageIndex = getSelectedStageByButton(game, level, pos);
			Stage selectedStage = getStage(selectedStageIndex);
			if (selectedStage == null) {
				return InteractionResult.PASS;
			}

			ScavengerHuntState state = player.getData(LoveTropicsAttachments.SCAVENGER_HUNT_STATE);
			tryUnlock(player, selectedStageIndex, state, selectedStage);
			getScoreAccess(player).set(state.nextStageIndex());

			return InteractionResult.PASS;
		});
	}

	private void tryUnlock(ServerPlayer player, int selectedStageIndex, ScavengerHuntState state, Stage selectedStage) {
		if (selectedStageIndex < state.nextStageIndex()) {
			// Already unlocked this stage
			alreadyUnlockedAction.ifPresent(action -> action.execute(player, state));
			selectedStage.alreadyUnlockedAction.ifPresent(action -> action.execute(player, state));
			return;
		}
		if (selectedStageIndex > state.nextStageIndex()) {
			// Haven't unlocked prerequisite
			missingPreviousAction.ifPresent(action -> action.execute(player, state));
			selectedStage.missingPreviousAction.ifPresent(action -> action.execute(player, state));
			return;
		}

		state.incrementStageIndex();
		unlockAction.ifPresent(action -> action.execute(player, state));
		selectedStage.unlockAction.ifPresent(action -> action.execute(player, state));
	}

	private @Nullable Stage getStage(int index) {
		return index >= 0 && index < stages.size() ? stages.get(index) : null;
	}

	private int getSelectedStageByButton(PersistentGame game, ServerLevel level, BlockPos pos) {
		for (int i = 0; i < stages.size(); i++) {
			Stage stage = stages.get(i);
			if (stage.buttonRegion.contains(game, level, pos)) {
				return i;
			}
		}
		return NO_STAGE;
	}

	private ScoreAccess getScoreAccess(ServerPlayer serverPlayer) {
		Objective objective = getScoreObjective(serverPlayer);
		return objective.getScoreboard().getOrCreatePlayerScore(serverPlayer, objective);
	}

	private Objective getScoreObjective(ServerPlayer serverPlayer) {
		return PersistentScoreboards.getOrCreateObjective(serverPlayer.level(), "scavenger_hunt", "next_stage");
	}

	@Override
	public MapCodec<ScavengerHuntBehavior> codec() {
		return CODEC;
	}

	public record Stage(
			RegionQuery buttonRegion,
			Optional<Action> unlockAction,
			Optional<Action> missingPreviousAction,
			Optional<Action> alreadyUnlockedAction
	) {
		public static final Codec<Stage> CODEC = RecordCodecBuilder.create(i -> i.group(
				RegionQuery.CODEC.fieldOf("button_region").forGetter(Stage::buttonRegion),
				Action.CODEC.optionalFieldOf("on_unlock").forGetter(Stage::unlockAction),
				Action.CODEC.optionalFieldOf("on_missing_previous").forGetter(Stage::missingPreviousAction),
				Action.CODEC.optionalFieldOf("on_already_unlocked").forGetter(Stage::alreadyUnlockedAction)
		).apply(i, Stage::new));
	}

	public record Action(
			List<String> commands,
			Optional<Integer> setStageIndex,
			List<Component> messages,
			Optional<Holder<SoundEvent>> sound
	) {
		public static final Codec<Action> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.listOf().optionalFieldOf("commands", List.of()).forGetter(Action::commands),
				ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("set_stage_index").forGetter(Action::setStageIndex),
				ComponentSerialization.CODEC.listOf().optionalFieldOf("messages", List.of()).forGetter(Action::messages),
				SoundEvent.CODEC.optionalFieldOf("sound").forGetter(Action::sound)
		).apply(i, Action::new));

		public void execute(ServerPlayer player, ScavengerHuntState state) {
			if (!commands.isEmpty()) {
				CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput().withPermission(LevelBasedPermissionSet.ADMIN).withSource(player.level().getServer());
				CommandDispatcher<CommandSourceStack> dispatcher = source.dispatcher();
				for (String command : commands) {
					try {
						dispatcher.execute(command, source);
					} catch (CommandSyntaxException e) {
						LOGGER.error("Failed to run command {}: {}", command, e.getMessage());
					}
				}
			}

			setStageIndex.ifPresent(state::setNextStageIndex);

			for (Component message : messages) {
				player.sendSystemMessage(message);
			}

			sound.ifPresent(s -> Util.sendNotifySound(player, s.value(), SoundSource.PLAYERS, 1.0f, 1.0f));
		}
	}
}
