package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class GameEndEffectsBehavior implements IGameBehavior {
	private static final long NO_STOP_DELAY = -1L;

	public static final MapCodec<GameEndEffectsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.LONG.optionalFieldOf("stop_delay", NO_STOP_DELAY).forGetter(c -> c.stopDelay),
			MoreCodecs.long2Object(TemplatedText.CODEC).optionalFieldOf("scheduled_messages", new Long2ObjectOpenHashMap<>()).forGetter(c -> c.scheduledMessages),
			Podium.CODEC.optionalFieldOf("podium").forGetter(c -> c.podium)
	).apply(i, GameEndEffectsBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private final long stopDelay;
	private final Long2ObjectMap<TemplatedText> scheduledMessages;
	private final Optional<Podium> podium;

	private boolean ended;
	private long stopTime;

	@Nullable
	private Component winner;

	public GameEndEffectsBehavior(long stopDelay, Long2ObjectMap<TemplatedText> scheduledMessages, Optional<Podium> podium) {
		this.stopDelay = stopDelay;
		this.scheduledMessages = scheduledMessages;
		this.podium = podium;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			this.winner = ComponentUtils.mergeStyles(winner.name().copy(), Style.EMPTY.withColor(ChatFormatting.AQUA));
			if (!ended) {
				triggerEnd(game);
			}
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (ended) {
				tickEnded(game);
			}
		});
	}

	private void triggerEnd(IGamePhase game) {
		ended = true;

		podium.ifPresent(p -> setupPodium(game, p));
	}

	private void setupPodium(IGamePhase game, Podium podium) {
		MapRegions regions = game.mapRegions();
		GameStatistics statistics = game.statistics();

		MutablePlayerSet players = new MutablePlayerSet(game.server());
		game.allPlayers().forEach(players::add);

		for (ServerPlayer player : players) {
			game.setPlayerRole(player, null);
		}

		for (PlayerKey playerKey : statistics.getPlayers()) {
			ServerPlayer player = players.getPlayerBy(playerKey);
			if (player == null) {
				continue;
			}

			int placement = statistics.forPlayer(playerKey).getOr(StatisticKey.PLACEMENT, Integer.MAX_VALUE);
			if (placement >= 1 && placement <= podium.winnerRegions().size()) {
				String regionKey = podium.winnerRegions().get(placement - 1);
				BlockBox region = regions.getAny(regionKey);
				if (region != null) {
					teleportToRegion(player, region);
					players.remove(playerKey.id());
				} else {
					LOGGER.error("Failed to find podium region '{}'", regionKey);
				}
			}
		}

		BlockBox loserRegion = regions.getAny(podium.loserRegion());
		if (loserRegion == null) {
			LOGGER.error("Failed to find loser region '{}'", podium.loserRegion());
			return;
		}

		for (ServerPlayer player : players) {
			teleportToRegion(player, loserRegion);
		}
	}

	private static void teleportToRegion(ServerPlayer player, BlockBox region) {
		Vec3 pos = region.center();
		player.teleportTo(player.level(), pos.x(), pos.y(), pos.z(), Set.of(), 0.0f, 0.0f, true);
	}

	private void tickEnded(IGamePhase game) {
		sendScheduledMessages(game, stopTime);

		if (stopDelay != NO_STOP_DELAY && stopTime == stopDelay) {
			game.requestStop(GameStopReason.finished());
		}

		stopTime++;
	}

	private void sendScheduledMessages(IGamePhase game, long stopTime) {
		TemplatedText message = scheduledMessages.remove(stopTime);
		if (message != null) {
			game.allPlayers().sendMessage(message.apply(Map.of("winner", winner)));
		}
	}

	public record Podium(List<String> winnerRegions, String loserRegion) {
		public static final Codec<Podium> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.listOf().fieldOf("winner_regions").forGetter(Podium::winnerRegions),
				Codec.STRING.fieldOf("loser_region").forGetter(Podium::loserRegion)
		).apply(i, Podium::new));
	}
}
