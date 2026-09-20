package org.lovetropics.games.common.content.trash_dive;

import org.lovetropics.games.common.content.block.TrashType;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.util.Util;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

public final class TrashCollectionBehavior implements IGameBehavior {
	public static final MapCodec<TrashCollectionBehavior> CODEC = MapCodec.unit(TrashCollectionBehavior::new);

	private final Set<Block> trashBlocks;

	private boolean gameOver;

	public TrashCollectionBehavior() {
		TrashType[] trashTypes = TrashType.values();
		trashBlocks = new ReferenceOpenHashSet<>();

		for (TrashType trash : trashTypes) {
			trashBlocks.add(trash.get());
		}
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.START, initiator -> onStart(game));
		events.listen(GamePhaseEvents.FINISH, () -> triggerGameOver(game));
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (role == PlayerRole.PARTICIPANT) {
				onAddPlayer(game, player);
			}
		});
		events.listen(GamePlayerEvents.LEFT_CLICK_BLOCK, (player, level, pos) -> onPlayerLeftClickBlock(game, player, pos));
		events.listen(GamePlayerEvents.BREAK_BLOCK, this::onPlayerBreakBlock);

		events.listen(GameLogicEvents.GAME_OVER, winner -> triggerGameOver(game));
	}

	private void onStart(IGamePhase game) {
		PlayerSet players = game.participants();
		players.addPotionEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
	}

	private void onAddPlayer(IGamePhase game, ServerPlayer player) {
		GameStatistics statistics = game.statistics();
		statistics.forPlayer(player).set(StatisticKey.TRASH_COLLECTED, 0);
	}

	private void onPlayerLeftClickBlock(IGamePhase game, ServerPlayer player, BlockPos pos) {
		ServerLevel level = player.level();

		BlockState state = level.getBlockState(pos);
		if (!isTrash(state)) {
			return;
		}

		level.removeBlock(pos, false);
		Util.sendNotifySound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);

		GameStatistics statistics = game.statistics();
		statistics.forPlayer(player)
				.withDefault(StatisticKey.TRASH_COLLECTED, () -> 0)
				.apply(collected -> collected + 1);
	}

	private TriState onPlayerBreakBlock(ServerPlayer player, BlockPos pos, BlockState state, InteractionHand hand) {
		return isTrash(state) ? TriState.DEFAULT : TriState.FALSE;
	}

	private boolean isTrash(BlockState state) {
		return trashBlocks.contains(state.getBlock());
	}

	private void triggerGameOver(IGamePhase game) {
		if (gameOver) {
			return;
		}

		gameOver = true;

		int totalSeconds = (int) (game.ticks() / SharedConstants.TICKS_PER_SECOND);
		game.statistics().global().set(StatisticKey.TOTAL_TIME, totalSeconds);
	}
}
