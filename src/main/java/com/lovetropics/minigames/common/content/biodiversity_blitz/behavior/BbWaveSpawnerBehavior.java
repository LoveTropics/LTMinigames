package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobSpawner;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BbWaveSpawnerBehavior implements IGameBehavior {
	public static final MapCodec<BbWaveSpawnerBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.LONG.fieldOf("interval_seconds").forGetter(c -> c.intervalTicks / 20),
			Codec.LONG.fieldOf("warn_seconds").forGetter(c -> c.warnTicks / 20),
			SizeCurve.CODEC.fieldOf("size_curve").forGetter(c -> c.sizeCurve),
			Codec.BOOL.fieldOf("size_curve_always").orElse(false).forGetter(c -> c.sizeCurveAlways),
			MoreCodecs.object2Float(Difficulty.CODEC).fieldOf("difficulty_factors").forGetter(c -> c.difficultyFactors),
			ComponentSerialization.CODEC.optionalFieldOf("first_message", CommonComponents.EMPTY).forGetter(c -> c.firstMessage),
			IGameBehavior.CODEC.listOf().optionalFieldOf("children", List.of()).forGetter(c -> c.children)
	).apply(i, BbWaveSpawnerBehavior::new));

	private final long intervalTicks;
	private final long warnTicks;
	private final SizeCurve sizeCurve;
	private final boolean sizeCurveAlways;

	private final Object2FloatMap<Difficulty> difficultyFactors;

	private final Component firstMessage;
	private final List<IGameBehavior> children;

	private IGamePhase game;
	private GameEventListeners listeners;
	private TeamState teams;
	private PlotsState plots;

	private int sentWaves = 0;
	private final Map<UUID, List<WaveTracker>> waveTrackers = new HashMap<>();
	private ServerBossEvent waveCharging;

	public BbWaveSpawnerBehavior(long intervalSeconds, long warnSeconds, SizeCurve sizeCurve, boolean sizeCurveAlways, Object2FloatMap<Difficulty> difficultyFactors, Component firstMessage, List<IGameBehavior> children) {
		intervalTicks = intervalSeconds * 20;
		warnTicks = warnSeconds * 20;
		this.sizeCurve = sizeCurve;
		this.sizeCurveAlways = sizeCurveAlways;

		this.difficultyFactors = difficultyFactors;
		this.children = children;
		this.difficultyFactors.defaultReturnValue(1.0F);

		this.firstMessage = firstMessage;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;

		teams = game.instanceState().getOrThrow(TeamState.KEY);
		plots = game.state().getOrThrow(PlotsState.KEY);

		events.listen(BbEvents.ASSIGN_PLOT, this::addPlayer);
		events.listen(GamePlayerEvents.REMOVE, this::removePlayer);

		events.listen(GamePhaseEvents.TICK, this::tick);
		events.listen(GamePhaseEvents.STOP, reason -> {
			waveTrackers.values().stream()
					.flatMap(List::stream)
					.forEach(WaveTracker::close);
			cleanupBossBar(waveCharging);
		});

		listeners = new GameEventListeners();
		final var cl = events.redirect(e -> e == BbEvents.MODIFY_WAVE_MODS, listeners);
		children.forEach(child -> child.register(game, cl));

		waveCharging = new ServerBossEvent(BiodiversityBlitzTexts.WAVE_WARNING, BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
		waveCharging.setProgress(0.0F);
		waveCharging.setVisible(false);
	}

	private void addPlayer(ServerPlayer player, Plot plot) {
		waveCharging.addPlayer(player);
	}

	private void removePlayer(ServerPlayer player) {
		waveCharging.removePlayer(player);
		List<WaveTracker> waves = waveTrackers.remove(player.getUUID());
		if (waves != null) {
			for (WaveTracker wave : waves) {
				wave.close();
			}
		}
	}

	private void tick() {
		ServerLevel world = game.level();
		RandomSource random = world.getRandom();
		long ticks = game.ticks();

		long timeTilNextWave = ticks % intervalTicks;

		if (timeTilNextWave == intervalTicks - warnTicks) {
			game.participants().sendMessage(BiodiversityBlitzTexts.WAVE_WARNING, true);
		}

		if (timeTilNextWave > intervalTicks - 40) {
			waveCharging.setProgress(1.0F - (intervalTicks - timeTilNextWave) / 40.0F);
			waveCharging.setVisible(true);
		}

		if (timeTilNextWave == 0) {
			waveCharging.setVisible(false);

			for (Plot plot : plots) {
				PlayerSet players = teams.getPlayersForTeam(game, plot.team);
				spawnWave(world, random, players, plot, sentWaves);
			}

			sentWaves++;
		}

		waveTrackers.forEach((pid, waves) -> {
			Iterator<WaveTracker> iterator = waves.iterator();
			while (iterator.hasNext()) {
				WaveTracker wave = iterator.next();
				if (tickWave(wave)) {
					wave.close();
					iterator.remove();
				}
			}
		});
	}

	private boolean tickWave(WaveTracker wave) {
		if (wave.entities.removeIf(e -> !e.isAlive())) {
			wave.bar.setProgress((float) wave.entities.size() / wave.waveSize);
		}
		return wave.entities.isEmpty();
	}

	private void cleanupBossBar(ServerBossEvent bar) {
		bar.removeAllPlayers();
	}

	private void spawnWave(ServerLevel world, RandomSource random, PlayerSet players, Plot plot, int waveIndex) {
		if (waveIndex == 0 && firstMessage != CommonComponents.EMPTY) {
			players.sendMessage(firstMessage);
		}

		Difficulty difficulty = world.getDifficulty();
		float difficultyFactor = difficultyFactors.getFloat(difficulty);

		double fractionalCount = sizeCurve.apply(waveIndex, difficultyFactor);
		int count = Mth.floor(fractionalCount);
		if (random.nextDouble() > fractionalCount - count) {
			count++;
		}

		if (!sizeCurveAlways) {
			int currencyIncr = plot.nextCurrencyIncrement;
			if (currencyIncr < 5) {
				count = random.nextInt(2) + 1;
			} else if (currencyIncr < 10) {
				count = (int) Mth.lerp(currencyIncr / 10.0, 2, count);
			}
		}

		// Early on, only spawn at the "base"
		Set<Entity> entities = BbMobSpawner.spawnWaveEntities(world, random,
				plot, count, waveIndex, BbMobSpawner::selectEntityForWave, listeners.invoker(BbEvents.MODIFY_WAVE_MODS));

		for (ServerPlayer player : players) {
			ServerBossEvent bossBar = createWaveBar(player, waveIndex, count, entities);

			WaveTracker wave = new WaveTracker(bossBar, entities);
			waveTrackers.computeIfAbsent(player.getUUID(), $ -> new ArrayList<>()).add(wave);
		}
	}

	private ServerBossEvent createWaveBar(ServerPlayer player, int waveIndex, int count, Set<Entity> entities) {
		ServerBossEvent bossBar = new ServerBossEvent(BiodiversityBlitzTexts.WAVE_NUMBER.apply(waveIndex + 1), BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
		bossBar.setProgress((float) entities.size() / count);
		bossBar.setColor(BossBarColor.GREEN);
		bossBar.addPlayer(player);

		return bossBar;
	}

	static final class WaveTracker {
		final ServerBossEvent bar;
		final Set<Entity> entities;
		final int waveSize;

		WaveTracker(ServerBossEvent bar, Set<Entity> entities) {
			this.bar = bar;
			this.entities = entities;
			waveSize = entities.size();
		}

		void close() {
			bar.removeAllPlayers();
		}
	}

	record SizeCurve(double lower, double upper, double base, double scale) {
		public static final MapCodec<SizeCurve> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.DOUBLE.fieldOf("lower").forGetter(c -> c.lower),
				Codec.DOUBLE.fieldOf("upper").forGetter(c -> c.upper),
				Codec.DOUBLE.fieldOf("base").forGetter(c -> c.base),
				Codec.DOUBLE.fieldOf("scale").forGetter(c -> c.scale)
		).apply(i, SizeCurve::new));

		// Desmos: https://www.desmos.com/calculator/ya880ablya
		double apply(int index, float difficulty) {
			double lower = this.lower * difficulty;
			double range = upper - lower;
			double base = this.base * difficulty;
			double scale = this.scale;
			return lower + range * (1.0 - Math.pow(base, (-index * scale) / range));
		}
	}
}
