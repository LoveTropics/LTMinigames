package com.lovetropics.minigames.common.content.build_battle;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.Overlords;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.lovetropics.minigames.common.core.game.util.SelectorItems;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BuildBattleBehavior implements IGameBehavior {
	private final String plotRegionsName;
	private long buildTime;
	private long reviewTime;

	public static final MapCodec<BuildBattleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("plots_name").forGetter(c -> c.plotRegionsName),
			Codec.LONG.fieldOf("build_time").orElse((long) (5 * 60 * SharedConstants.TICKS_PER_SECOND)).forGetter(c -> c.buildTime)
	).apply(i, BuildBattleBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private final HashMap<UUID, BlockBox> playerPlots = new HashMap<>();
	private final HashMap<UUID, Integer> playerPoints = new HashMap<>();
	private final List<UUID> reviewedPlayers = new java.util.ArrayList<>();
	private final HashMap<UUID, Integer> overlordPoints = new HashMap<>();
	private int revieweeIndex = -1;
	private Overlords overlords;
	private SelectorItems<Integer> selectorItems;

	private GameBossBar bar;
	private boolean building = true;

	public BuildBattleBehavior(String plotRegionsName, long buildTime) {
		this.plotRegionsName = plotRegionsName;
		this.buildTime = buildTime;
		this.reviewTime = buildTime + 10 * SharedConstants.TICKS_PER_SECOND;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		selectorItems = new SelectorItems<>(new VoteItemsHandlers(this), new Integer[]{1, 2, 3, 4, 5, 6});

		GameWidgets widgets = GameWidgets.getOrRegister(game, events);

		events.listen(GamePhaseEvents.START, initiator -> {
			bar = widgets.openBossBar(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
			game.allPlayers().forEach(bar::addPlayer);
			refreshBuildingTimeBar(0);
			building = true;
			overlords = Overlords.get(game);
			game.allPlayers().sendMessage(BuildBattleTexts.BUILDING_START.copy().withStyle(ChatFormatting.YELLOW));
		});
		events.listen(GamePhaseEvents.DESTROY, () -> {
			bar.close();
		});
		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() <= buildTime) {
				refreshBuildingTimeBar(game.ticks());
			}
			if (game.ticks() == buildTime) {
				game.allPlayers().sendMessage(BuildBattleTexts.BUILDING_END.copy().withStyle(ChatFormatting.YELLOW));
				building = false;
				bar.close();
				for (var player : game.allPlayers()) {
					player.getInventory().clearContent();
				}
			}
			if (game.ticks() == (buildTime + 5 * SharedConstants.TICKS_PER_SECOND)) {
				for (ServerPlayer player : game.participants().shuffledCopy(game.random())) {
					game.setPlayerRole(player, PlayerRole.SPECTATOR);
				}
				game.allPlayers().sendMessage(BuildBattleTexts.REVIEW_TIME.copy().withStyle(ChatFormatting.YELLOW));
			}
			if (game.ticks() == reviewTime) {
				nextReviewee(game, widgets);
			}

			game.participants().forEach(player -> {
				if (!playerPlots.containsKey(player.getUUID())) {
					return;
				}
				SpawnBuilder spawn = new SpawnBuilder(player);
				BlockBox plot;
				if (this.revieweeIndex > -1) {
					plot = playerPlots.getOrDefault(reviewedPlayers.get(revieweeIndex), null);
				} else {
					plot = playerPlots.get(player.getUUID());
				}
				if (plot == null) {
					LOGGER.error("Player {} has no plot assigned!", reviewedPlayers.get(revieweeIndex));
					return;
				}
				if (!plot.contains(player.blockPosition())) {
					game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, null);
					spawn.teleportAndApply(player);
				}
			});
		});

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> assignPlots(participants, game.mapRegions().get(plotRegionsName)));
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(game.level(), playerId, spawn));

		// players cannot interact with blocks outside their plot
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> canInteract(player.getUUID(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.PLACE_BLOCK, (player, pos, placed, placedOn, placedItemStack) -> canInteract(player.getUUID(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, result) -> canInteract(player.getUUID(), pos) ? InteractionResult.PASS : InteractionResult.FAIL);
		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> onUseItem(game, player, hand, widgets));

		selectorItems.applyTo(events);
	}

	public void assignPlots(Set<PlayerKey> players, Collection<BlockBox> plots) {
		if (plots.size() < players.size()) {
			throw new GameException(Component.literal("Not enough plots for all players in Build Battle game (" + plots.size() + " plots named \"" + plotRegionsName + "\" for " + players.size() + " players)"));
		}
		var iterator = players.iterator();
		for (BlockBox plot : plots) {
			if (!iterator.hasNext()) {
				break;
			}
			var player = iterator.next();
			playerPlots.put(player.id(), plot);
			playerPoints.put(player.id(), -1);
		}
	}

	public boolean canInteract(UUID playerId, BlockPos pos) {
		return building && playerPlots.containsKey(playerId) && playerPlots.get(playerId).contains(pos);
	}

	private void spawnPlayer(ServerLevel level, UUID playerId, SpawnBuilder builder) {
		if (this.revieweeIndex > -1) {
			var plot = playerPlots.getOrDefault(reviewedPlayers.get(revieweeIndex), null);
			if (plot == null) {
				LOGGER.error("Player {} has no plot assigned!", reviewedPlayers.get(revieweeIndex));
				return;
			}
			builder.teleportTo(level, tryFindEmptyPos(level, level.getRandom(), plot));
			return;
		}
		var plot = playerPlots.getOrDefault(playerId, null);
		if (plot != null) {
			builder.teleportTo(level, tryFindEmptyPos(level, level.getRandom(), plot));
		}
	}

	private BlockPos tryFindEmptyPos(ServerLevel level, RandomSource random, BlockBox box) {
		for (int i = 0; i < 20; i++) {
			BlockPos pos = box.sample(random);
			if (level.isEmptyBlock(pos)) {
				return pos;
			}
		}
		LOGGER.debug("USING FALLBACK SPAWN POS");
		return box.centerBlock();
	}

	private void refreshBuildingTimeBar(long ticks) {
		var remaining = buildTime - ticks;
		var minutes = (remaining / (60 * SharedConstants.TICKS_PER_SECOND)) % 60;
		var seconds = (remaining / SharedConstants.TICKS_PER_SECOND) % 60;

		bar.setTitle(BuildBattleTexts.BAR_BUILDING.copy().withStyle(ChatFormatting.AQUA).append(" ").append(BuildBattleTexts.COUNTDOWN.apply(String.format("%02d", minutes), String.format("%02d", seconds))));
		bar.setProgress(buildTime > 0 ? (float) remaining / (float) buildTime : 0f);
	}

	private void nextReviewee(IGamePhase game, GameWidgets widgets) {
		if (revieweeIndex == -1) {
			for (UUID playerId : playerPoints.keySet()) {
				if (playerPoints.get(playerId) == -1) {
					reviewedPlayers.add(playerId);
					revieweeIndex = 0;
					refreshReviewee(game, widgets);
					return;
				}
			}
			return;
		}
		var currentRevieweeId = reviewedPlayers.get(revieweeIndex);
		revieweeIndex++;

		if (revieweeIndex == reviewedPlayers.size()) {
			if (currentRevieweeId != null) {
				var points = 0;
				for (var overlordPoints : this.overlordPoints.entrySet()) {
					points += overlordPoints.getValue();
				}
				playerPoints.put(currentRevieweeId, points);
			}
			this.overlordPoints.clear();

			for (UUID playerId : playerPoints.keySet()) {
				if (playerPoints.get(playerId) == -1) {
					reviewedPlayers.add(playerId);
					refreshReviewee(game, widgets);
					return;
				}
			}
		}
		if (revieweeIndex < reviewedPlayers.size()) {
			refreshReviewee(game, widgets);
			return;
		}
		revieweeIndex = -2;
		announceWinner(game);
	}

	private void previousReviewee(IGamePhase game, GameWidgets widgets) {
		if (revieweeIndex <= 0) {
			return;
		}
		revieweeIndex--;
		refreshReviewee(game, widgets);
	}

	private void announceWinner(IGamePhase game) {
		// display leaderboard
		var max = 5;
		for (Map.Entry<UUID, Integer> entry : playerPoints.entrySet()) {
			ServerPlayer player = game.allPlayers().getPlayerBy(entry.getKey());
			if (player != null) {
				game.statistics().forPlayer(player).incrementInt(StatisticKey.POINTS, entry.getValue());
			}
		}

		var leaderboard = new java.util.ArrayList<>(playerPoints.entrySet());
		leaderboard.sort((a, b) -> b.getValue().compareTo(a.getValue()));
		var message = BuildBattleTexts.RESULTS.copy().append("\n").append("\n");
		for (int i = 0; i < Math.min(max, leaderboard.size()); i++) {
			var entry = leaderboard.get(i);
			var playerName = "unknown player";
			var player = game.level().getPlayerByUUID(entry.getKey());
			if (player != null) {
				playerName = player.getScoreboardName();
				message.append(Component.literal(String.valueOf(i + 1)).withStyle(ChatFormatting.GRAY).append(" ").append(BuildBattleTexts.POINTS_DISPLAY.apply(playerName, entry.getValue())).append("\n"));
			}
		}

		for (var player : game.allPlayers()) {
			player.sendSystemMessage(message, false);
			player.getInventory().clearContent();
		}

		bar.close();

		// TODO: tp to plot of winning player
	}

	private void refreshReviewee(IGamePhase game, GameWidgets widgets) {
		for (var player : game.allPlayers()) {
			SpawnBuilder spawn = new SpawnBuilder(player);
			game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, null);
			spawn.teleportAndApply(player);
		}
		for (var overlord : overlords) {
			overlord.getInventory().clearContent();
			var previous = new ItemStack(Items.SPONGE);
			previous.set(DataComponents.CUSTOM_NAME, BuildBattleTexts.ITEM_PREVIOUS);
			overlord.addItem(previous);
			if (revieweeIndex == reviewedPlayers.size() - 1) {
				selectorItems.giveSelectorsTo(overlord);
			}
			var next = new ItemStack(Items.GOLD_BLOCK);
			next.set(DataComponents.CUSTOM_NAME, BuildBattleTexts.ITEM_NEXT);
			overlord.addItem(next);
		}

		//TODO put participants in spectator

		var playerName = "unknown player";
		var player = game.level().getPlayerByUUID(reviewedPlayers.get(revieweeIndex));
		if (player != null) {
			playerName = player.getScoreboardName();
		}

		bar.close();
		bar = widgets.openBossBar(BuildBattleTexts.BAR_REVIEWING.apply(playerName).withStyle(ChatFormatting.YELLOW), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
		bar.setProgress(revieweeIndex / (float) playerPoints.size());
		game.allPlayers().forEach(bar::addPlayer);
	}

	private InteractionResult onUseItem(IGamePhase game, ServerPlayer player, InteractionHand hand, GameWidgets widgets) {
		if (this.revieweeIndex < 0 || !overlords.contains(player.getUUID())) {
			return InteractionResult.PASS;
		}
		ItemStack heldStack = player.getItemInHand(hand);
		if (heldStack.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (heldStack.is(Items.SPONGE)) {
			previousReviewee(game, widgets);
			return InteractionResult.SUCCESS;
		}
		if (heldStack.is(Items.GOLD_BLOCK)) {
			nextReviewee(game, widgets);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	private record VoteItemsHandlers(BuildBattleBehavior behavior) implements SelectorItems.Handlers<Integer> {
		@Override
		public void onPlayerSelected(ServerPlayer player, Integer value) {
			if (behavior.overlords.contains(player.getUUID())) {
				behavior.overlordPoints.put(player.getUUID(), value);
				player.sendSystemMessage(BuildBattleTexts.GIVE_POINTS.apply(value).withStyle(ChatFormatting.GOLD), false);
			}
		}

		@Override
		public String getIdFor(Integer value) {
			return String.valueOf(value);
		}

		@Override
		public Component getNameFor(Integer value) {
			return BuildBattleTexts.ITEM_POINTS.apply(value);
		}

		@Override
		public Item getItemFor(Integer value) {
			return switch (value) {
				case 1 -> Items.WOOL.brown();
				case 2 -> Items.WOOL.red();
				case 3 -> Items.WOOL.lime();
				case 4 -> Items.WOOL.green();
				case 5 -> Items.WOOL.blue();
				case 6 -> Items.WOOL.yellow();

				default -> Items.WOOL.white();
			};
		}
	}
}
