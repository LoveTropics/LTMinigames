package org.lovetropics.games.common.content.hoedown;

import com.lovetropics.lib.item.StackedHat;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePackageEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.MinigameCompetitionEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.region.LevelBlockPos;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.integration.game_actions.Donation;
import org.lovetropics.games.common.util.EntityTemplate;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record HatsBehavior(
		List<HatMobSpec> hatMobs,
		RegionQuery hatSpawn,
		WeightedList<StackedHat> hats,
		double hatDonationValue,
		float loseOnWin
) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	// TODO: Tune all the values based on expected player count
	public static final MapCodec<HatsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(HatMobSpec.CODEC.listOf()).fieldOf("hat_mobs").forGetter(HatsBehavior::hatMobs),
			RegionQuery.CODEC.fieldOf("hat_spawn").forGetter(HatsBehavior::hatSpawn),
			WeightedList.nonEmptyCodec(StackedHat.CODEC).fieldOf("hats").forGetter(HatsBehavior::hats),
			Codec.DOUBLE.fieldOf("hat_donation_value").forGetter(HatsBehavior::hatDonationValue),
			Codec.FLOAT.optionalFieldOf("lose_on_win", 1.0f).forGetter(HatsBehavior::loseOnWin)
	).apply(i, HatsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		HatMobSelector mobSelector = new HatMobSelector(hatMobs);
		State state = new State(mobSelector);

		// TODO: Should we run the spawner while in the lobby?
		events.listen(GamePackageEvents.RECEIVE_DONATION, donation -> {
			String name = donation.selectRandomName(game.random());
			state.accumulatedDonations += donation.amount();
			state.donorNamePool.add(name, donation.amount());
		});

		events.listen(MinigameCompetitionEvents.RETURN_TO_LOBBY, () ->
				tryRunMobSpawner(game, state)
		);

		events.listen(GameLivingEntityEvents.DAMAGE_AMOUNT, (level, entity, damageSource, amount, originalAmount) -> {
			if (damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				return amount;
			}
			if (!state.spawnedHatMobs.containsKey(entity.getUUID())) {
				return amount;
			} else if (!(damageSource.getEntity() instanceof ServerPlayer)) {
				return 0.0f;
			}
			return Math.max(amount, 1.0f);
		});

		events.listen(GameLivingEntityEvents.DEATH, (level, entity, damageSource) -> {
			if (damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				return TriState.DEFAULT;
			}
			HatMobSpec hatMob = state.spawnedHatMobs.remove(entity.getUUID());
			if (hatMob != null) {
				// TODO: Count amount of damage done by each player..?
				ServerPlayer responsiblePlayer = getResponsiblePlayer(game, level, entity, damageSource);
				if (responsiblePlayer == null) {
					// Nobody to attribute the hats to, just restore the entity
					return TriState.FALSE;
				}
				addHatValueToPlayer(state, hatMob, responsiblePlayer);
			}
			return TriState.DEFAULT;
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> commands.register(
				Commands.literal("lobby_hats")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("runspawn")
								.executes(_ -> {
									tryRunMobSpawner(game, state);
									return 1;
								})
						)
						.then(Commands.literal("fakedonation")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
										.executes(context -> {
											game.invoker(GamePackageEvents.RECEIVE_DONATION).onReceiveDonation(new Donation(
													"LoveTropics",
													DoubleArgumentType.getDouble(context, "amount"),
													"",
													false,
													0.0,
													"LoveTropics",
													Util.NIL_UUID
											));
											return 1;
										})
								)
						)
		));

		events.listen(GamePlayerEvents.ADD, player -> {
			if (state.scheduledWinners.remove(player.getUUID())) {
				handleWinnerInLobby(game, player);
			}
		});
		events.listen(GamePlayerEvents.REMOVE, player ->
				state.transferHats.put(player.getUUID(), List.copyOf(StackedHat.getEntityHats(player)))
		);
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			subEvents.listen(GamePlayerEvents.ADD, player -> {
				List<StackedHat> hats = state.transferHats.remove(player.getUUID());
				if (hats != null) {
					StackedHat.setEntityHats(player, hats);
				} else {
					// TODO ???
					LOGGER.error("No hats for {} when adding to game", player.getScoreboardName());
				}
			});
			subEvents.listen(GameLogicEvents.GAME_OVER, winner -> {
				for (PlayerKey player : winner.asPlayerKeys(game)) {
					state.scheduledWinners.add(player.id());
				}
			});
		});
	}

	// TODO: Some kind of effects to indicate that a bunch of hats spawned
	private void tryRunMobSpawner(IGamePhase game, State state) {
		if (state.accumulatedDonations <= 0.0) {
			return;
		}

		state.mobSelector.reset();
		List<HatMobSpec> selection = state.mobSelector.run(state.accumulatedDonations);
		Collections.shuffle(selection);

		for (HatMobSpec mob : selection) {
			state.accumulatedDonations -= mob.donationValue;

			String name = state.donorNamePool.select(mob.donationValue);
			// Can probably happen, in a rounding error
			if (name == null) {
				// TODO: Translation?
				name = "A generous donor";
			}

			// TODO: Show $ amount in name tag
			spawnMobWithHat(game, state, Component.literal(name), mob);
		}
	}

	private void addHatValueToPlayer(State state, HatMobSpec mob, ServerPlayer player) {
		PlayerState playerState = state.players.computeIfAbsent(player.getUUID(), _ -> new PlayerState());

		// TODO: Sound + notify
		playerState.collectedHatValue += mob.donationValue;
		while (playerState.collectedHatValue >= hatDonationValue) {
			StackedHat.addHatToEntity(player, hats.getRandomOrThrow(player.getRandom()));
			playerState.collectedHatValue -= hatDonationValue;
		}
	}

	private void handleWinnerInLobby(IGamePhase game, ServerPlayer player) {
		if (loseOnWin > 0.0f) {
			dropHats(game, player, loseOnWin);
		}
	}

	private void dropHats(IGamePhase game, ServerPlayer player, float fraction) {
		// TODO: Some notification text
		RandomSource random = player.getRandom();
		int hatsToRemove = Mth.ceil(fraction * StackedHat.getEntityHats(player).size());
		if (hatsToRemove == 0) {
			return;
		} else if (hatsToRemove == 1) {
			dropOneHat(player, random);
			return;
		}

		int defaultDropInterval = SharedConstants.TICKS_PER_SECOND / 2;
		int dropDuration = Math.min(SharedConstants.TICKS_PER_SECOND * 10, hatsToRemove * defaultDropInterval);
		for (int i = 0; i < hatsToRemove; i++) {
			game.scheduler().runAfterTicks(
					Math.round((float) i / (hatsToRemove - 1) * dropDuration),
					() -> dropOneHat(player, random)
			);
		}
	}

	private void dropOneHat(ServerPlayer player, RandomSource random) {
		List<StackedHat> hats = new ArrayList<>(StackedHat.getEntityHats(player));
		if (hats.isEmpty()) {
			return;
		}
		StackedHat hat = hats.remove(random.nextInt(hats.size()));
		StackedHat.setEntityHats(player, hats);
		ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getEyeY(), player.getZ(), hat.item().create());
		itemEntity.setDeltaMovement(random.triangle(0.0, 0.25), 0.2, random.triangle(0.0, 0.25));
		itemEntity.setNeverPickUp();
		itemEntity.lifespan = SharedConstants.TICKS_PER_SECOND * 3;
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_STAND_FALL, SoundSource.PLAYERS);
		player.level().addFreshEntity(itemEntity);
	}

	private @Nullable ServerPlayer getResponsiblePlayer(IGamePhase game, ServerLevel level, LivingEntity entity, DamageSource damageSource) {
		if (damageSource.getEntity() instanceof ServerPlayer player) {
			return player;
		} else if (damageSource.getDirectEntity() instanceof ServerPlayer player) {
			return player;
		}
		return game.participants().inLevel(level).stream()
				.min(Comparator.comparingDouble(player -> player.distanceToSqr(entity)))
				.orElse(null);
	}

	// TODO: Don't spawn inside blocks
	private void spawnMobWithHat(IGamePhase game, State state, Component customName, HatMobSpec spec) {
		RandomSource random = game.random();
		LevelBlockPos randomPos = hatSpawn.pickRandomPosAtHeightmap(game, random, Heightmap.Types.WORLD_SURFACE);
		if (randomPos == null) {
			LOGGER.warn("No position to spawn mob with hat in {}", hatSpawn);
			return;
		}

		BlockPos pos = randomPos.pos();
		Entity entity = spec.entity.spawn(randomPos.level(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0f, 0.0f);
		if (entity == null) {
			return;
		}

		state.spawnedHatMobs.put(entity.getUUID(), spec);

		entity.setCustomName(customName);
		entity.setCustomNameVisible(true);

		if (entity instanceof Mob mob) {
			mob.setPersistenceRequired();
		}
		if (entity instanceof LivingEntity living) {
			AttributeInstance scaleInstance = living.getAttribute(Attributes.SCALE);
			if (scaleInstance != null) {
				scaleInstance.setBaseValue(scaleInstance.getValue() * spec.scale);
			}
			living.skipDropExperience();
		}

		StackedHat.setEntityHats(entity, List.of(hats.getRandomOrThrow(random)));
	}

	@Override
	public MapCodec<HatsBehavior> codec() {
		return CODEC;
	}

	private static class State {
		private final HatMobSelector mobSelector;
		private final DonorNamePool donorNamePool = new DonorNamePool();

		private double accumulatedDonations;

		private final Map<UUID, PlayerState> players = new HashMap<>();

		private final Map<UUID, List<StackedHat>> transferHats = new HashMap<>();
		private final Set<UUID> scheduledWinners = new HashSet<>();

		private final Map<UUID, HatMobSpec> spawnedHatMobs = new HashMap<>();

		private State(HatMobSelector mobSelector) {
			this.mobSelector = mobSelector;
		}
	}

	private static class PlayerState {
		private double collectedHatValue;
	}

	private static class HatMobSelector {
		private final Reference2IntOpenHashMap<Option> selectedCount = new Reference2IntOpenHashMap<>();
		private int totalSelectedCount;

		public HatMobSelector(List<HatMobSpec> mobs) {
			float totalOptionWeight = (float) mobs.stream().mapToDouble(o -> o.weight).sum();
			for (HatMobSpec mob : mobs) {
				Option option = new Option(mob, mob.weight / totalOptionWeight);
				selectedCount.put(option, 0);
			}
		}

		public void reset() {
			for (Reference2IntMap.Entry<Option> entry : selectedCount.reference2IntEntrySet()) {
				entry.setValue(0);
			}
			totalSelectedCount = 0;
		}

		public List<HatMobSpec> run(double availableValue) {
			List<HatMobSpec> selection = new ArrayList<>();
			while (availableValue > 0.0) {
				HatMobSpec mob = selectOne(availableValue);
				// Nothing left that's viable to add, leave the value remainder over
				if (mob == null) {
					break;
				}
				availableValue -= mob.donationValue;
				selection.add(mob);
			}
			return selection;
		}

		/// Select based on the available funds, such that the ratio between all spawned mob types is as close as possible to the configured target.
		private @Nullable HatMobSpec selectOne(double availableValue) {
			Option bestOption = null;
			float bestError = Float.MAX_VALUE;

			for (Option candidate : selectedCount.keySet()) {
				if (candidate.mob.donationValue > availableValue) {
					continue;
				}
				float error = 0.0f;
				for (Reference2IntMap.Entry<Option> entry : selectedCount.reference2IntEntrySet()) {
					Option option = entry.getKey();
					error += option.errorFromTarget(
							entry.getIntValue() + (candidate == option ? 1 : 0),
							totalSelectedCount + 1
					);
				}
				if (error < bestError) {
					bestOption = candidate;
					bestError = error;
				}
			}

			if (bestOption == null) {
				return null;
			}

			selectedCount.addTo(bestOption, 1);
			totalSelectedCount++;

			return bestOption.mob;
		}

		private record Option(HatMobSpec mob, float targetFraction) {
			private float errorFromTarget(int count, int totalCount) {
				float fraction = (float) count / totalCount;
				return Mth.square(fraction - targetFraction);
			}
		}
	}

	/// Selects donor names proportionally to how much they donated
	private static class DonorNamePool {
		private final List<NamedPool> pools = new ArrayList<>();

		public void add(String name, double value) {
			for (NamedPool pool : pools) {
				if (pool.name.equals(name)) {
					pool.value += value;
					return;
				}
			}
			pools.add(new NamedPool(name, value));
		}

		public @Nullable String select(double value) {
			// Certainly not the nicest way to do this, but ok
			Collections.shuffle(pools);
			for (int i = 0; i < pools.size(); i++) {
				NamedPool pool = pools.get(i);
				if (pool.value < value) {
					continue;
				}
				pool.value -= value;
				if (pool.value <= 0.0) {
					pools.remove(i);
				}
				return pool.name;
			}
			return null;
		}

		private static class NamedPool {
			private final String name;
			private double value;

			private NamedPool(String name, double value) {
				this.name = name;
				this.value = value;
			}
		}
	}

	public record HatMobSpec(
			EntityTemplate entity,
			float scale,
			double donationValue,
			float weight
	) {
		public static final Codec<HatMobSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
				EntityTemplate.MAP_CODEC.forGetter(HatMobSpec::entity),
				Codec.FLOAT.fieldOf("scale").forGetter(HatMobSpec::scale),
				Codec.DOUBLE.fieldOf("donation_value").forGetter(HatMobSpec::donationValue),
				Codec.FLOAT.fieldOf("weight").forGetter(HatMobSpec::weight)
		).apply(i, HatMobSpec::new));
	}
}
