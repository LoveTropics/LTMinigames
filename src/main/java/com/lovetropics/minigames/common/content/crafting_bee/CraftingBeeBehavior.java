package com.lovetropics.minigames.common.content.crafting_bee;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.crafting_bee.ingredient.IngredientDecomposer;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameWinner;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.CraftingBeeCraftsClientState;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.TimedGameState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamConfig;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CraftingBeeBehavior implements IGameBehavior {
	public static final MapCodec<CraftingBeeBehavior> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			RecipeSelector.CODEC.listOf().fieldOf("selectors").forGetter(c -> c.selectors),
			IngredientDecomposer.CODEC.codec().listOf().fieldOf("decomposers").forGetter(c -> c.decomposers),
			Codec.INT.optionalFieldOf("hints_per_player", 3).forGetter(c -> c.allowedHints),
			TemplatedText.CODEC.fieldOf("timer_bar").forGetter(c -> c.timerBarText),
			Codec.INT.fieldOf("time_per_team").forGetter(t -> t.timePerTeam),
			Codec.INT.fieldOf("recycling_penalty").forGetter(t -> t.recyclingPenalty),
			Codec.STRING.fieldOf("recycling_region").forGetter(c -> c.recyclingRegionKey),
			Codec.unboundedMap(GameTeamKey.CODEC, TeamRegions.CODEC).fieldOf("team_regions").forGetter(b -> b.teamRegions)
	).apply(in, CraftingBeeBehavior::new));

	private final List<RecipeSelector> selectors;
	private final List<IngredientDecomposer> decomposers;
	private final int allowedHints;

	private final TemplatedText timerBarText;
	private final int timePerTeam, recyclingPenalty;
	private final String recyclingRegionKey;
	private final Map<GameTeamKey, TeamRegions> teamRegions;

	private TeamState teams;
	private IGamePhase game;
	private List<BlockBox> recyclingRegions;

	private ListMultimap<GameTeamKey, CraftingTask> tasks;
	private volatile boolean done;

	private Map<GameTeamKey, CraftingTeamState> teamStates;
	private Set<GameTeamKey> teamsWithoutTime;

	public CraftingBeeBehavior(List<RecipeSelector> selectors, List<IngredientDecomposer> decomposers, int allowedHints, TemplatedText timerBarText, int timePerTeam, int recyclingPenalty, String recyclingRegionKey, Map<GameTeamKey, TeamRegions> teamRegions) {
		this.selectors = selectors;
		this.decomposers = decomposers;
		this.allowedHints = allowedHints;

		this.timerBarText = timerBarText;
		this.timePerTeam = timePerTeam;
		this.recyclingPenalty = recyclingPenalty;
		this.recyclingRegionKey = recyclingRegionKey;
		this.teamRegions = teamRegions;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		tasks = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);

		decomposers.forEach(dec -> dec.prepareCache(game.level()));

		this.game = game;
		teams = game.instanceState().getOrThrow(TeamState.KEY);

		recyclingRegions = game.mapRegions().getAll(recyclingRegionKey);

		teamStates = new HashMap<>();
		teamsWithoutTime = new HashSet<>();

		GameWidgets widgets = GameWidgets.getOrRegister(game, events);
		events.listen(GamePhaseEvents.START, initiator -> start(widgets));
		events.listen(GamePhaseEvents.TICK, () -> tickRunning(game));
		events.listen(GamePhaseEvents.DESTROY, () -> teamStates.values().forEach(b -> b.timerBar().close()));
		events.listen(GamePhaseEvents.STOP, reason -> teamStates.values().forEach(b -> b.timerBar().close()));

		events.listen(GameLogicEvents.GAME_OVER, this::onGameOver);

		events.listen(GamePlayerEvents.CRAFT_RESULT, this::modifyCraftResult);
		events.listen(GamePlayerEvents.CRAFT, this::onCraft);
		events.listen(GamePlayerEvents.USE_BLOCK, this::useBlock);
		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> InteractionResult.FAIL);

		events.listen(GamePhaseEvents.STOP, reason -> {
			game.allPlayers().forEach(ServerPlayer::closeContainer);
			GameClientState.removeFromPlayers(GameClientStateTypes.CRAFTING_BEE_CRAFTS.get(), game.allPlayers());
		});
		events.listen(GamePlayerEvents.REMOVE, player -> {
			player.closeContainer();
			GameClientState.removeFromPlayer(GameClientStateTypes.CRAFTING_BEE_CRAFTS.get(), player);
		});
	}

	private void start(GameWidgets widgets) {
		for (GameTeam team : teams) {
			var recipes = selectors.stream().map(selector -> selector.select(game.level()))
					.map(recipe -> new CraftingTask(
							CraftingBee.getCraftingRecipeResult(recipe.value(), game.registryAccess()),
							recipe
					))
					.toList();
			tasks.putAll(team.key(), recipes);
			sync(team.key());
			distributeIngredients(recipes, teams.getPlayersForTeam(game, team.key()));

			List<TaskDisplay> taskDisplays = setupTaskDisplays(Objects.requireNonNull(teamRegions.get(team.key())), recipes);

			GameBossBar timerBar = widgets.openBossBar(CommonComponents.EMPTY, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
			teams.getPlayersForTeam(game, team.key()).forEach(timerBar::addPlayer);

			GameBossBar taskBar = widgets.openBossBar(team.config().styledName(), team.config().bossBarColor(), BossEvent.BossBarOverlay.PROGRESS);
			taskBar.setProgress(0.0f);

			teamStates.put(team.key(), new CraftingTeamState(
					taskDisplays,
					getGlassBlockForTeam(team.config()),
					taskBar,
					timerBar,
					new TimedGameState(timePerTeam, 0)
			));
		}

		for (CraftingTeamState state : teamStates.values()) {
			widgets.registerWidget(state.taskBar, true);
		}
	}

	private List<TaskDisplay> setupTaskDisplays(TeamRegions teamRegions, List<CraftingTask> recipes) {
		BlockBox beaconRow = game.mapRegions().getOrThrow(teamRegions.beaconRow);
		BlockBox itemRow = game.mapRegions().getOrThrow(teamRegions.itemRow);

		List<BlockPos> beaconGlassPositions = BlockPos.betweenClosedStream(beaconRow.min(), beaconRow.max())
				.filter(pos -> game.level().getBlockState(pos).is(Tags.Blocks.GLASS_BLOCKS))
				.map(BlockPos::immutable)
				.toList();

		if (beaconGlassPositions.size() != recipes.size()) {
			throw new IllegalStateException("Only found " + beaconGlassPositions.size() + " beacons, but there are " + recipes.size() + " recipes");
		}

		Direction.Axis rowAxis = itemRow.size().getX() > beaconRow.size().getZ() ? Direction.Axis.X : Direction.Axis.Z;

		List<TaskDisplay> taskDisplays = new ArrayList<>(recipes.size());
		for (int i = 0; i < recipes.size(); i++) {
			BlockPos glassPos = beaconGlassPositions.get(i);
			BlockPos itemPos = itemRow.min().relative(rowAxis, glassPos.get(rowAxis) - beaconRow.min().get(rowAxis));
			Display.ItemDisplay itemDisplay = spawnItemDisplay(itemPos, recipes.get(i).output);
			taskDisplays.add(new TaskDisplay(glassPos, itemDisplay));
		}

		return taskDisplays;
	}

	private Display.ItemDisplay spawnItemDisplay(BlockPos displayPos, ItemStack displayItem) {
		Display.ItemDisplay itemDisplay = EntityType.ITEM_DISPLAY.create(game.level(), EntitySpawnReason.COMMAND);
		itemDisplay.setPos(Vec3.atCenterOf(displayPos));
		itemDisplay.setItemStack(displayItem.copy());
		itemDisplay.setTransformation(new Transformation(null, null, new Vector3f(0.5f), null));
		itemDisplay.setBillboardConstraints(Display.BillboardConstraints.VERTICAL);
		itemDisplay.setCustomNameVisible(true);
		itemDisplay.setCustomName(displayItem.getHoverName());
		itemDisplay.setBrightnessOverride(Brightness.FULL_BRIGHT);
		game.level().addFreshEntity(itemDisplay);
		return itemDisplay;
	}

	private void distributeIngredients(Collection<CraftingTask> tasks, PlayerSet players) {
		// Empty teams have no players to distribute items to
		if (players.isEmpty()) {
			return;
		}

		for (CraftingTask task : tasks) {
			var ingredients = task.recipe.value().placementInfo().ingredients();
			var items = ingredients.stream().flatMap(this::singleDecomposition).collect(net.minecraft.util.Util.toMutableList());
			Collections.shuffle(items);

			// Evenly distribute the items between the players
			int p = 0;
			var playerList = players.stream().toList();
			for (ItemStack item : items) {
				playerList.get(p++).addItem(item.copy());
				if (p >= playerList.size()) {
					p = 0;
				}
			}
		}
	}

	private Stream<ItemStack> singleDecomposition(Ingredient ingredient) {
		if (ingredient.isEmpty()) {
			return Stream.empty();
		}

		for (IngredientDecomposer decomposer : decomposers) {
			var decomposed = decomposer.decompose(ingredient);
			if (decomposed != null) {
				return decomposed.stream().flatMap(this::singleDecomposition);
			}
		}

		// We have reduced the ingredient to its most basic form, so now we just pick the first item of the ingredient
		for (Holder<Item> item : ingredient.getValues()) {
			// Prioritize vanilla items
			if (item.unwrapKey().filter(k -> k.identifier().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)).isPresent()) {
				return Stream.of(new ItemStack(item));
			}
		}
		return Stream.of(new ItemStack(ingredient.getValues().get(0)));
	}

	private ItemStack modifyCraftResult(ServerPlayer player, ItemStack result, CraftingInput craftingInput, CraftingRecipe recipe) {
		GameTeamKey team = teams.getTeamForPlayer(player);
		if (team == null || teamsWithoutTime.contains(team)) {
			// Don't allow players to use their inventory to craft, either!
			return ItemStack.EMPTY;
		}
		ItemContainerContents usedItems = ItemContainerContents.fromItems(craftingInput.items().stream()
				.filter(stack -> !stack.isEmpty())
				.map(stack -> stack.copyWithCount(1))
				// .peek(s -> s.remove(CraftingBee.CRAFTED_USING)) // TODO - should we keep this?
				.sorted(Comparator.comparing(s -> s.typeHolder().getRegisteredName()))
				.toList());
		result.set(CraftingBee.CRAFTED_USING, new CraftedUsing(
				result.getCount(),
				usedItems
		));
		return result;
	}

	private void onCraft(Player player, ItemStack crafted, Container container) {
		var team = teams.getTeamForPlayer(player);
		if (team == null || done) {
			return;
		}

		var teamTasks = tasks.get(team);

		var craftedStackToCompare = crafted.copy();
		craftedStackToCompare.remove(CraftingBee.CRAFTED_USING);
		var task = teamTasks.stream().filter(c -> ItemStack.isSameItemSameComponents(craftedStackToCompare, c.output)).findFirst().orElse(null);

		if (task == null || task.done) {
			return;
		}

		task.done = true;

		sync(team);

		var completed = teamTasks.stream().filter(t -> t.done).count();
		game.statistics().forTeam(team).set(StatisticKey.ITEMS_CRAFTED, (int) completed);

		var gameTeam = teams.getTeamByKey(team);
		var teamConfig = gameTeam.config();

		setTeamTaskProgress(teamConfig, team, (int) completed, teamTasks.size(), teamTasks.indexOf(task));

		PlayerSet teamPlayers = teams.getPlayersForTeam(game, team);
		teamPlayers.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.0f);
		game.spectators().playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.0f);

		PlayerSet otherPlayers = PlayerSet.difference(game.participants(), teamPlayers);
		otherPlayers.playSound(SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 0.5f, 0.5f);

		if (completed == teamTasks.size()) {
			triggerGameOver(new GameWinner.Team(gameTeam));
		}
	}

	private void setTeamTaskProgress(GameTeamConfig teamConfig, GameTeamKey team, int count, int totalCount, int taskIndex) {
		game.allPlayers().sendMessage(CraftingBeeTexts.TEAM_HAS_COMPLETED_RECIPES.apply(teamConfig.styledName(), count, totalCount));

		CraftingTeamState teamState = teamStates.get(team);
		float progress = count / (float) totalCount;
		teamState.taskBar.setProgress(progress);

		TaskDisplay taskDisplay = teamState.taskDisplays.get(taskIndex);
		BlockPos glass = taskDisplay.beaconGlassPos;
		game.level().setBlockAndUpdate(glass, teamState.beaconGlass);
		taskDisplay.itemDisplay.setBrightnessOverride(new Brightness(0, 13));
	}

	private static BlockState getGlassBlockForTeam(GameTeamConfig teamConfig) {
		for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.GLASS_BLOCKS)) {
			if (block.value() instanceof StainedGlassBlock stainedGlass && stainedGlass.getColor() == teamConfig.dye()) {
				return stainedGlass.defaultBlockState();
			}
		}
		return Blocks.GLASS.defaultBlockState();
	}

	private void triggerGameOver(GameWinner winner) {
		game.invoker(GameLogicEvents.GAME_OVER).onGameOver(winner);
	}

	private void onGameOver(GameWinner winner) {
		done = true;
		game.allPlayers().forEach(ServerPlayer::closeContainer);
	}

	private void tickRunning(IGamePhase game) {
		teamStates.forEach((team, bar) -> {
			switch (bar.state.tick(game.ticks())) {
				case RUNNING -> {
					long ticksRemaining = bar.state.getTicksRemaining();
					if (ticksRemaining % SharedConstants.TICKS_PER_SECOND == 0) {
						bar.timerBar.setTitle(getTimeRemainingText(game, ticksRemaining));
						bar.timerBar.setProgress((float) ticksRemaining / timePerTeam);
					}
				}
				case GAME_OVER -> {
					bar.timerBar.setTitle(getTimeRemainingText(game, 0));
					bar.timerBar.setProgress(0);

					teamsWithoutTime.add(team);

					PlayerSet playersInTeam = teams.getPlayersForTeam(game, team);
					playersInTeam.forEach(ServerPlayer::closeContainer);
					playersInTeam.playSound(SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.5f, 0.25f);
					playersInTeam.showTitle(CraftingBeeTexts.TIME_UP, null, 10, SharedConstants.TICKS_PER_SECOND * 2, 10);

					if (teamsWithoutTime.size() == tasks.asMap().size()) {
						int mx = tasks.asMap().values().stream().mapToInt(craftingTasks -> (int) craftingTasks.stream().filter(c -> c.done).count())
								.max().orElse(0);
						var withMax = tasks.asMap().entrySet().stream().filter(e -> e.getValue().stream().filter(c -> c.done).count() == mx)
								.toList();
						if (withMax.size() != 1) {
							triggerGameOver(new GameWinner.Nobody());
						} else {
							var gameTeam = teams.getTeamByKey(withMax.getFirst().getKey());
							triggerGameOver(new GameWinner.Team(gameTeam));
						}
					}
				}
			}
		});
	}

	private Component getTimeRemainingText(IGamePhase game, long ticksRemaining) {
		long secondsRemaining = ticksRemaining / (long) game.level().tickRateManager().tickrate();

		Component timeText = Component.literal(Util.formatMinutesSeconds(secondsRemaining)).withStyle(ChatFormatting.AQUA);
		Component gameNameText = game.definition().name().copy().withStyle(ChatFormatting.AQUA);

		return timerBarText.apply(Map.of("time", timeText, "game", gameNameText));
	}

	private InteractionResult useBlock(ServerPlayer player, ServerLevel world, BlockPos pos, InteractionHand hand, BlockHitResult traceResult) {
		// don't allow players to use the crafting table after the game was won
		if (world.getBlockState(pos).is(Blocks.CRAFTING_TABLE) && (done || teamsWithoutTime.contains(teams.getTeamForPlayer(player)))) {
			return InteractionResult.FAIL;
		}
		ItemStack itemInHand = player.getItemInHand(hand);
		for (BlockBox recycler : recyclingRegions) {
			if (recycler.contains(pos) && !itemInHand.isEmpty()) {
				return useRecycler(player, itemInHand);
			}
		}
		return InteractionResult.PASS;
	}

	private InteractionResult useRecycler(ServerPlayer player, ItemStack itemToRecycle) {
		CraftedUsing craftedUsing = itemToRecycle.get(CraftingBee.CRAFTED_USING);
		if (craftedUsing == null) {
			player.sendSystemMessage(CraftingBeeTexts.CANNOT_RECYCLE, true);
			com.lovetropics.minigames.common.util.Util.sendNotifySound(player, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0f, 1.0f);
			return InteractionResult.FAIL;
		}
		if (itemToRecycle.getCount() < craftedUsing.count()) {
			player.sendSystemMessage(CraftingBeeTexts.NOT_ENOUGH_TO_RECYCLE.apply(craftedUsing.count(), itemToRecycle.getHoverName()), true);
			com.lovetropics.minigames.common.util.Util.sendNotifySound(player, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0f, 1.0f);
			return InteractionResult.FAIL;
		}

		itemToRecycle.shrink(craftedUsing.count());
		craftedUsing.items().nonEmptyItemCopyStream().forEach(player::addItem);

		applyTimePenalty(player, recyclingPenalty);
		com.lovetropics.minigames.common.util.Util.sendNotifySound(player, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f, 1.0f);

		return InteractionResult.SUCCESS;
	}

	private void applyTimePenalty(ServerPlayer player, int penalty) {
		teamStates.get(teams.getTeamForPlayer(player))
				.state().increaseRemaining(-penalty);

		Component subtitle = CraftingBeeTexts.TIME_PENALTY.apply(Mth.positiveCeilDiv(penalty, SharedConstants.TICKS_PER_SECOND));
		teams.getPlayersOnSameTeam(game, player).showTitle(null, subtitle, 10, 20, 10);
	}

	private void sync(GameTeamKey team) {
		teams.getPlayersForTeam(game, team).forEach(this::sync);
	}

	private void sync(Player player) {
		if (player instanceof ServerPlayer sp) {
			GameClientState.sendToPlayer(new CraftingBeeCraftsClientState(tasks.get(teams.getTeamForPlayer(player)).stream().map(CraftingTask::toCraft).toList(), allowedHints), sp);
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return CraftingBee.CRAFTING_BEE;
	}

	private static class CraftingTask {
		private final ItemStack output;
		private final RecipeHolder<?> recipe;
		private boolean done;

		private CraftingTask(ItemStack output, RecipeHolder<?> recipe) {
			this.output = output;
			this.recipe = recipe;
		}

		public CraftingBeeCraftsClientState.Craft toCraft() {
			return new CraftingBeeCraftsClientState.Craft(output, recipe.id(), recipe.value().display().getFirst(), done);
		}
	}

	private record TeamRegions(
			String beaconRow,
			String itemRow
	) {
		public static final Codec<TeamRegions> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("beacon_row").forGetter(TeamRegions::beaconRow),
				Codec.STRING.fieldOf("item_row").forGetter(TeamRegions::itemRow)
		).apply(i, TeamRegions::new));
	}

	private record CraftingTeamState(
			List<TaskDisplay> taskDisplays,
			BlockState beaconGlass,
			GameBossBar taskBar,
			GameBossBar timerBar,
			TimedGameState state
	) {
	}

	private record TaskDisplay(BlockPos beaconGlassPos, Display.ItemDisplay itemDisplay) {
	}
}
