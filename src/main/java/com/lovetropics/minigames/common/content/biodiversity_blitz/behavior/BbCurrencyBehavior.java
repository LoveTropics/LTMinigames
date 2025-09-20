package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.tutorial.TutorialState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.CurrencyItemState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.CurrencyManager;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantFamily;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantType;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.stream.IntStream;

public final class BbCurrencyBehavior implements IGameBehavior {
	// TODO: configurable?
	private static final Object2FloatMap<Difficulty> DEATH_DECREASE = new Object2FloatOpenHashMap<>();

	static {
		DEATH_DECREASE.put(Difficulty.EASY, 0.9f);
		DEATH_DECREASE.put(Difficulty.NORMAL, 0.8F);
		DEATH_DECREASE.put(Difficulty.HARD, 0.5F);
	}

	public static final MapCodec<BbCurrencyBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(c -> c.item),
			Codec.INT.fieldOf("initial_currency").forGetter(c -> c.initialCurrency),
			DropCalculation.CODEC.fieldOf("drop_calculation").forGetter(c -> c.dropCalculation),
			Codec.LONG.fieldOf("drop_interval").forGetter(c -> c.dropInterval)
	).apply(i, BbCurrencyBehavior::new));

	private final Item item;
	private final int initialCurrency;

	private final DropCalculation dropCalculation;
	private final long dropInterval;

	private IGamePhase game;
	private CurrencyManager currency;
	private TutorialState tutorial;

	public BbCurrencyBehavior(Item item, int initialCurrency, DropCalculation dropCalculation, long dropInterval) {
		this.initialCurrency = initialCurrency;
		this.item = item;
		this.dropCalculation = dropCalculation;
		this.dropInterval = dropInterval;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		currency = phaseState.register(CurrencyManager.KEY, new CurrencyManager(game, item));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		tutorial = game.state().getOrThrow(TutorialState.KEY);

		GameClientState.applyGlobally(new CurrencyItemState(item.getDefaultInstance()), events);

		events.listen(BbEvents.ASSIGN_PLOT, (player, plot) -> {
			currency.set(player, initialCurrency);
		});

		events.listen(GamePlayerEvents.THROW_ITEM, (player, item) -> {
			ItemStack stack = item.getItem();
			if (stack.getItem() == this.item) {
				return TriState.FALSE;
			}
			return TriState.DEFAULT;
		});

		events.listen(GamePhaseEvents.TICK, () -> currency.tickTracked());

		events.listen(BbEvents.TICK_PLOT, this::tickPlot);
		events.listen(BbEvents.BB_DEATH, this::onPlayerDeath);
	}

	private void tickPlot(Plot plot, PlayerSet players) {
		if (!tutorial.isTutorialFinished()) {
			return;
		}

		long ticks = game.ticks();

		if (ticks % SharedConstants.TICKS_PER_SECOND == 0) {
			int nextCurrencyIncrement = computeNextCurrency(plot);
			if (plot.nextCurrencyIncrement != nextCurrencyIncrement) {
				plot.nextCurrencyIncrement = nextCurrencyIncrement;
				game.invoker(BbEvents.CURRENCY_INCREMENT_CHANGED).onCurrencyChanged(plot.team, nextCurrencyIncrement, plot.nextCurrencyIncrement);
			}
		}

		long intervalTicks = dropInterval * SharedConstants.TICKS_PER_SECOND;
		if (ticks % intervalTicks == 0) {
			int amount = plot.nextCurrencyIncrement;
			currency.accumulate(plot.team, amount);
			for (ServerPlayer player : players) {
				giveCurrency(player, amount);
			}
		}
	}

	private void giveCurrency(ServerPlayer player, int amount) {
		int count = currency.add(player, amount);
		player.displayClientMessage(BiodiversityBlitzTexts.currencyAddition(count), true);
		if (count > 0) {
			player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.24F, 1.0F);
		}
	}

	private int computeNextCurrency(Plot plot) {
		double value = computePlotValue(plot);
		return Math.max(Mth.floor(value), 1);
	}

	private double computePlotValue(Plot plot) {
		PlotMetrics metrics = PlotMetrics.compute(plot.plants);

		double value = 1.0;
		for (PlantFamily family : PlantFamily.BIODIVERSITY_VALUES) {
			double familyValue = metrics.valuesByFamily.getDouble(family);
			double diversity = metrics.diversityByFamily.getDouble(family);
			value += dropCalculation.applyFamily(familyValue, diversity);
		}

		return dropCalculation.applyGlobal(value);
	}

	private TriState onPlayerDeath(ServerPlayer player, DamageSource damageSource) {
		// Resets all currency from the player's inventory and adds a new stack with 80% of the amount.
		// A better way of just removing 20% of the existing stacks could be done but this was chosen for the time being to save time
		Difficulty difficulty = game.level().getDifficulty();

		int oldCurrency = currency.get(player);
		int newCurrency = Mth.floor(oldCurrency * DEATH_DECREASE.getFloat(difficulty));

		if (oldCurrency != newCurrency) {
			currency.set(player, newCurrency);

//			player.sendStatusMessage(BiodiversityBlitzTexts.deathDecrease(oldCurrency - newCurrency).mergeStyle(TextFormatting.RED), true);
			player.connection.send(new ClientboundSetSubtitleTextPacket(BiodiversityBlitzTexts.deathDecrease(oldCurrency - newCurrency).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)));
		}

		return TriState.DEFAULT;
	}

	private record DropCalculation(double base, double bound, double diversityFactor) {
		public static final MapCodec<DropCalculation> CODEC = RecordCodecBuilder.mapCodec(instance -> {
			return instance.group(
					Codec.DOUBLE.fieldOf("base").forGetter(c -> c.base),
					Codec.DOUBLE.fieldOf("bound").forGetter(c -> c.bound),
					Codec.DOUBLE.fieldOf("diversity_factor").forGetter(c -> c.diversityFactor)
			).apply(instance, DropCalculation::new);
		});

		// The goal is to create a distribution that feels logarithmic.
		// When plots are first starting out, we should give more points per value increase of the plot.
		// This keeps the game feeling like linear progress in the beginning.
		// However, our shop scaling doesn't work with linear progress overall. To remedy this, the curve falls off
		// gradually to make it more and more difficult to get increasing amounts of points per turn.
		// the 'base' variable is how sharp this falloff is. Higher values get to the final value faster, whereas
		// lower 'base' values makes the curve more gradual. The final value itself is governed by the 'bound' variable.

		public double applyFamily(double familyValue, double diversity) {
			return familyValue * (1.0 + diversityFactor * diversity);
		}

		public double applyGlobal(double value) {
			return bound - (bound * Math.pow(base, -value / bound));
		}
	}

	private record PlotMetrics(Reference2DoubleMap<PlantFamily> valuesByFamily, Reference2DoubleMap<PlantFamily> diversityByFamily) {

		private static PlotMetrics compute(Iterable<Plant> plants) {
				// Temp fix for plant types not knowing the plant value
				Object2DoubleOpenHashMap<PlantType> valueByType = new Object2DoubleOpenHashMap<>();
				Reference2DoubleOpenHashMap<PlantFamily> values = new Reference2DoubleOpenHashMap<>();

				// Number of plants per plant type in family
				Reference2ObjectMap<PlantFamily, Object2IntOpenHashMap<PlantType>> numberOfPlants = new Reference2ObjectOpenHashMap<>();

				for (Plant plant : plants) {
					// Increment plant type

					// Negative or zero value marks plants that shouldn't count towards biodiversity
					if (plant.value() > 0.0) {
						numberOfPlants.computeIfAbsent(plant.family(), f -> new Object2IntOpenHashMap<>())
								.addTo(plant.type(), 1);

						valueByType.put(plant.type(), plant.value());
					}
				}

				Reference2DoubleMap<PlantFamily> diversity = new Reference2DoubleOpenHashMap<>();

				for (Reference2ObjectMap.Entry<PlantFamily, Object2IntOpenHashMap<PlantType>> entry : Reference2ObjectMaps.fastIterable(numberOfPlants)) {
					// Total amount of plants in this family
					int total = IntStream.of(entry.getValue().values().toIntArray()).sum();

					// Amount of plant types
					int types = entry.getValue().size();

					double biodiversity = 0.0;

					// The target is to have an equal amount of plant types in the family
					int target = total / types;

					// Scale the plants, so when you have a lot of a single plant type and less of another, the biodiversity is proportional to that
					for (Object2IntMap.Entry<PlantType> e : Object2IntMaps.fastIterable(entry.getValue())) {
						int plantCount = e.getIntValue();
						double ratio = (double) plantCount / target;
						double percent = (double) plantCount / total;
						biodiversity += Math.max(1, ratio);

						int amountThatShouldCount = plantCount;
						if (total > entry.getKey().getMinBeforeMonoculture()) {
							if (percent > 0.5) {
								// Monoculture detected, only count bottom half
								amountThatShouldCount = plantCount / 2;
							}
						}

						// Multiply the amount of plants by their value to get the total value for this type, add that to the family
						values.addTo(entry.getKey(), amountThatShouldCount * valueByType.getDouble(e.getKey()));
					}

					diversity.put(entry.getKey(), biodiversity);
				}

				return new PlotMetrics(values, diversity);
			}
		}
}
