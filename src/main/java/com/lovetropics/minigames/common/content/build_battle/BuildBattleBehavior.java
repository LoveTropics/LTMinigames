package com.lovetropics.minigames.common.content.build_battle;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.Overlords;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.lovetropics.minigames.common.core.game.util.SelectorItems;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
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
import java.util.Set;
import java.util.UUID;

public final class BuildBattleBehavior implements IGameBehavior {
	private final String plotRegionsName;
	private long buildTime;
	private long reviewTime;

	public static final MapCodec<BuildBattleBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("plot_name").forGetter(c -> c.plotRegionsName),
			Codec.LONG.fieldOf("build_time").orElse((long) (10 * 60 * SharedConstants.TICKS_PER_SECOND)).forGetter(c -> c.buildTime)
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
		selectorItems = new SelectorItems<>(new VoteItemsHandlers(this), new Integer[]{1,2,3,4,5,6});

		GameWidgets widgets = GameWidgets.getOrRegister(game, events);

		events.listen(GamePhaseEvents.START, initiator -> {
			bar = widgets.openBossBar(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
			game.allPlayers().forEach(bar::addPlayer);
			refreshBuildingTimeBar(0);
			building = true;
			overlords = Overlords.get(game);
		});
		events.listen(GamePhaseEvents.DESTROY, () -> {
			bar.close();
		});
		events.listen(GamePhaseEvents.TICK, () -> {
			if(game.ticks() <= buildTime) {
				refreshBuildingTimeBar(game.ticks());
			}
			if(game.ticks() == buildTime) {
				game.allPlayers().sendMessage(Component.literal("Building phase has ended!")); //TODO: translate
				building = false;
				bar.close();
				//TODO: clear players inventory
			}
			if(game.ticks() == (buildTime + 5 * SharedConstants.TICKS_PER_SECOND)) {
				game.allPlayers().sendMessage(Component.literal("Time for the jury to review your wonderful creations..."));

				//TODO temporary for testing
				game.participants().forEach(overlords::add);
			}
			if(game.ticks() == reviewTime) {
				nextReviewee(game, widgets);
			}
			if(game.ticks() >= reviewTime) {
				// review logic
			}

			game.participants().forEach(player -> {
				//TODO: force participants to stay inside plot
			});
		});

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> assignPlots(participants, game.mapRegions().get(plotRegionsName)));
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(game.level(), playerId, spawn));

		// players cannot interact with blocks outside their plot
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> canInteract(player.getUUID(), pos) ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.PLACE_BLOCK, (player, pos, placed, placedOn, placedItemStack) -> canInteract(player.getUUID(), pos)  ? TriState.DEFAULT : TriState.FALSE);
		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, result) -> canInteract(player.getUUID(), pos) ? InteractionResult.PASS : InteractionResult.FAIL);
		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> onUseItem(game, player, hand, widgets));

		selectorItems.applyTo(events);
	}

	public void assignPlots(Set<PlayerKey> players, Collection<BlockBox> plots) {
		if(plots.size() < players.size()) {
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
		if(this.revieweeIndex > -1) {
			var plot = playerPlots.getOrDefault(reviewedPlayers.get(revieweeIndex), null);
			if(plot == null) {
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
		//TODO: translate
		bar.setTitle(Component.literal("Building time! " + String.format("%02d:%02d", minutes, seconds) + " remaining"));
		bar.setProgress(buildTime > 0 ? (float) remaining / (float) buildTime : 0f);
	}

	private void nextReviewee(IGamePhase game, GameWidgets widgets) {
		if(revieweeIndex == -1) {
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

		if(revieweeIndex == reviewedPlayers.size()) {
			if(currentRevieweeId != null) {
				var points = 0;
				for(var overlordPoints : this.overlordPoints.entrySet()) {
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
		if(revieweeIndex < reviewedPlayers.size()) {
			refreshReviewee(game, widgets);
			return;
		}
		revieweeIndex = -2;
		announceWinner(game);
	}

	private void previousReviewee(IGamePhase game, GameWidgets widgets) {
		if(revieweeIndex <= 0) {
			return;
		}
		revieweeIndex--;
		refreshReviewee(game, widgets);
	}

	private void announceWinner(IGamePhase game) {
		// display leaderboard
		var max = 5;
		var leaderboard = new java.util.ArrayList<>(playerPoints.entrySet());
		leaderboard.sort((a, b) -> b.getValue().compareTo(a.getValue()));
		var message = Component.literal("Build Battle Results:\n"); //TODO: translate
		for(int i = 0; i < Math.min(max, leaderboard.size()); i++) {
			var entry = leaderboard.get(i);
			var playerName = "unknown player";
			var player = game.level().getPlayerByUUID(entry.getKey());
			if(player != null) {
				playerName = player.getScoreboardName();
				message.append(Component.literal(i + 1 + ". " + playerName + ": " + entry.getValue() + " points\n"));
			}
		}

		message.append(Component.literal("please close the game manually i haven't finished this yet")); //TODO

		for(var player : game.allPlayers()) {
			player.displayClientMessage(message, false);
			player.getInventory().clearContent();
		}

		bar.close();

		// TODO: tp to plot of winning player
	}

	private void refreshReviewee(IGamePhase game, GameWidgets widgets) {
		for(var player : game.allPlayers()) {
			SpawnBuilder spawn = new SpawnBuilder(player);
			game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, null);
			spawn.teleportAndApply(player);
		}
		for(var overlord : overlords) {
			overlord.getInventory().clearContent();
			overlord.addItem(new ItemStack(Items.SPONGE));
			if(revieweeIndex == reviewedPlayers.size() - 1) {
				selectorItems.giveSelectorsTo(overlord);
			}
			overlord.addItem(new ItemStack(Items.GOLD_BLOCK));
		}

		//TODO put participants in spectator

		var playerName = "unknown player";
		var player = game.level().getPlayerByUUID(reviewedPlayers.get(revieweeIndex));
		if(player != null) {
			playerName = player.getScoreboardName();
		}

		//TODO: translate
		bar.close();
		bar = widgets.openBossBar(Component.literal("Reviewing " + playerName + "..."), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
		bar.setProgress(1.0f);
		game.allPlayers().forEach(bar::addPlayer);
	}

	private InteractionResult onUseItem(IGamePhase game, ServerPlayer player, InteractionHand hand, GameWidgets widgets) {
		if(this.revieweeIndex < 0 || !overlords.contains(player.getUUID())) {
			return InteractionResult.PASS;
		}
		ItemStack heldStack = player.getItemInHand(hand);
		if (heldStack.isEmpty()) {
			return InteractionResult.PASS;
		}
		if(heldStack.is(Items.SPONGE)) {
			previousReviewee(game, widgets);
			return InteractionResult.SUCCESS;
		}
		if(heldStack.is(Items.GOLD_BLOCK)) {
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
				player.displayClientMessage(Component.literal("You gave " + value + " points!"), false);
			}
		}

		@Override
		public String getIdFor(Integer value) {
			return String.valueOf(value);
		}

		@Override
		public Component getNameFor(Integer value) {
			//TODO: translate
			return Component.literal(value + " points");
		}

		@Override
		public Item getItemFor(Integer value) {
			return switch (value) {
				case 1 -> Items.BROWN_WOOL;
				case 2 -> Items.RED_WOOL;
				case 3 -> Items.LIME_WOOL;
				case 4 -> Items.GREEN_WOOL;
				case 5 -> Items.BLUE_WOOL;
				case 6 -> Items.YELLOW_WOOL;

				default -> Items.WHITE_WOOL;
			};
		}
	}
}
