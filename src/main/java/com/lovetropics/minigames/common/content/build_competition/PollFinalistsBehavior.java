package com.lovetropics.minigames.common.content.build_competition;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.lovetropics.lib.techstack.Crud;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.integration.GameInstanceIntegrations;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public record PollFinalistsBehavior(String finalistsTag, String winnerTag, String votesObjective, String pollDuration) implements IGameBehavior {
	public static final MapCodec<PollFinalistsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("finalists_tag", "finalist").forGetter(c -> c.finalistsTag),
			Codec.STRING.optionalFieldOf("winner_tag", "winner").forGetter(c -> c.winnerTag),
			Codec.STRING.optionalFieldOf("votes_objective", "votes").forGetter(c -> c.votesObjective),
			Codec.STRING.optionalFieldOf("poll_duration", "5m").forGetter(c -> c.pollDuration)
	).apply(i, PollFinalistsBehavior::new));

	private static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameInstanceIntegrations integrations = game.getIntegrationsOrThrow();
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, commands -> {
			commands.registerAdmin("start_runoff", source -> {
				try {
					PlayerList players = source.getServer().getPlayerList();
					players.getPlayers().forEach(p -> p.removeTag(winnerTag));
					ObjectArrayList<String> finalists = players.getPlayers().stream()
							.filter(p -> p.getTags().contains(finalistsTag))
							.map(p -> p.getGameProfile().getName())
							.collect(ObjectArrayList.toList());
					Util.shuffle(finalists, RANDOM);
					integrations.createPoll("Choose the best build!", pollDuration, finalists.toArray(String[]::new));
				} catch (Exception e) {
					LOGGER.error("Failed to start runoff:", e);
				}
			});
		});

		events.listen(GamePackageEvents.RECEIVE_POLL_EVENT, (object, crud) -> handlePollEvent(game, object, crud));
	}

	private void handlePollEvent(IGamePhase game, JsonObject object, Crud crud) {
		MinecraftServer server = game.server();
		Optional<PollEvent> poll = PollEvent.CODEC.parse(JsonOps.INSTANCE, object).resultOrPartial(LOGGER::error);
		switch (crud) {
			case CREATE -> {
				LOGGER.info("New poll, resetting objective");
				Scoreboard scoreboard = server.getScoreboard();
				Objective objective = scoreboard.getObjective(votesObjective);
				if (objective != null) {
					scoreboard.removeObjective(objective);
				}
				scoreboard.addObjective(votesObjective, ObjectiveCriteria.DUMMY, Component.literal("Votes"), RenderType.INTEGER, true, null);
			}
			case UPDATE -> poll.ifPresent(event -> updateScores(server, event, false));
			case DELETE -> {
				LOGGER.info("Poll ended, finding winner");
				poll.ifPresent(event -> updateScores(server, event, true));
				ServerScoreboard scoreboard = server.getScoreboard();
				Objective objective = scoreboard.getObjective(votesObjective);
				Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(objective);
				if (!scores.isEmpty()) {
					String winner = Iterables.getLast(scores).owner();
					for (ServerPlayer player : server.getPlayerList().getPlayers()) {
						player.removeTag(finalistsTag);
						if (player.getGameProfile().getName().equals(winner)) {
							player.addTag(winnerTag);
						}
					}
				}
			}
		}
	}

	private static final RandomSource RANDOM = RandomSource.create();

	private void updateScores(MinecraftServer server, PollEvent event, boolean forceWinner) {
		Object2IntMap<String> votesByName = new Object2IntArrayMap<>();
		votesByName.defaultReturnValue(-1);
		for (Option option : event.options) {
			votesByName.put(option.title, option.results);
		}
		List<ServerPlayer> leaders = new ArrayList<>();
		int leaderVotes = 0;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			String username = player.getGameProfile().getName();
			int votes = votesByName.getInt(username);
			if (votes >= 0) {
				if (forceWinner) {
					if (votes > leaderVotes) {
						leaders.clear();
						leaderVotes = votes;
						leaders.add(player);
					} else if (votes == leaderVotes) {
						leaders.add(player);
					}
				}
				ServerScoreboard scoreboard = server.getScoreboard();
				Objective objective = scoreboard.getObjective(votesObjective);
				scoreboard.getOrCreatePlayerScore(player, objective).set(votes);
			}
		}
		if (leaders.size() > 1) {
			// Shhhhh
			ServerPlayer winner = Util.getRandom(leaders, RANDOM);
			ServerScoreboard scoreboard = server.getScoreboard();
			Objective objective = scoreboard.getObjective(votesObjective);
			scoreboard.getOrCreatePlayerScore(winner, objective).increment();
		}
	}

	public record PollEvent(int id, List<Option> options) {
		public static final Codec<PollEvent> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("id").forGetter(PollEvent::id),
				Option.CODEC.listOf().fieldOf("options").forGetter(PollEvent::options)
		).apply(i, PollEvent::new));
	}

	public record Option(char key, String title, int results) {
		private static final Codec<Character> CHARACTER_CODEC = Codec.STRING.comapFlatMap(
				s -> s.length() == 1 ? DataResult.success(s.charAt(0)) : DataResult.error(() -> "Expected single character, got " + s.length()),
				String::valueOf
		);

		public static final Codec<Option> CODEC = RecordCodecBuilder.create(i -> i.group(
				CHARACTER_CODEC.fieldOf("key").forGetter(Option::key),
				Codec.STRING.fieldOf("title").forGetter(Option::title),
				Codec.INT.fieldOf("results").forGetter(Option::results)
		).apply(i, Option::new));
	}
}
