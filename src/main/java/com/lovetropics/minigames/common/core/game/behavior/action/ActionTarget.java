package com.lovetropics.minigames.common.core.game.behavior.action;

import com.google.common.collect.Collections2;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface ActionTarget {
	Codec<ActionTarget> CODEC = Codec.lazyInitialized(() -> {
		Codec<ActionTarget> fullCodec = ActionTarget.REGISTRY.codec(Codec.STRING).dispatch(ActionTarget::codec, c -> c);
		Codec<ActionTarget> fullOrSequenceCodec = Codec.either(fullCodec, Sequence.INLINE_CODEC).xmap(
				Either::unwrap,
				target -> target instanceof Sequence sequence ? Either.right(sequence) : Either.left(target)
		);
		return Codec.either(fullOrSequenceCodec, Simple.CODEC).xmap(
				Either::unwrap,
				target -> target instanceof Simple simple ? Either.right(simple) : Either.left(target)
		);
	});

	ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ActionTarget>> REGISTRY = Util.make(new ExtraCodecs.LateBoundIdMapper<>(), ActionTarget::register);

	ActionTarget PASS = new Sequence(List.of());

	private static void register(ExtraCodecs.LateBoundIdMapper<String, MapCodec<? extends ActionTarget>> registry) {
		registry.put("simple", Simple.MAP_CODEC);
		registry.put("sequence", Sequence.MAP_CODEC);
		registry.put("exclude", Excluding.MAP_CODEC);
		registry.put("filter_entities", FilterEntities.MAP_CODEC);
		registry.put("players_around", PlayersAround.MAP_CODEC);
		registry.put("specific_player", SpecificPlayer.MAP_CODEC);
		registry.put("specific_team", SpecificTeam.MAP_CODEC);
	}

	ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources);

	MapCodec<? extends ActionTarget> codec();

	enum Simple implements ActionTarget, StringRepresentable {
		NONE("none", (game, sources) -> ActionSubjects.EMPTY),
		SOURCE("source", (game, sources) -> sources),
		ALL_PLAYERS("all_players", (game, sources) -> ActionSubjects.ofPlayers(game.allPlayers())),
		PARTICIPANTS("participants", (game, sources) -> ActionSubjects.ofPlayers(game.participants())),
		SPECTATORS("spectators", (game, sources) -> ActionSubjects.ofPlayers(game.spectators())),
		WIDEN_TO_TEAM("widen_to_team", (game, sources) -> sources.coerceInto(game, ActionSubjectType.TEAM)),
		WIDEN_TO_PLOT("widen_to_plot", (game, sources) -> sources.coerceInto(game, ActionSubjectType.PLOT)),
		;

		public static final Codec<Simple> CODEC = StringRepresentable.fromEnum(Simple::values);
		public static final MapCodec<Simple> MAP_CODEC = CODEC.fieldOf("value");

		private final String name;
		private final Resolver resolver;

		Simple(String name, Resolver resolver) {
			this.name = name;
			this.resolver = resolver;
		}

		@Override
		public ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources) {
			return resolver.resolveTargets(game, sources);
		}

		@Override
		public MapCodec<Simple> codec() {
			return MAP_CODEC;
		}

		@Override
		public String getSerializedName() {
			return name;
		}

		interface Resolver {
			ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources);
		}
	}

	record Sequence(List<ActionTarget> targets) implements ActionTarget {
		public static final MapCodec<Sequence> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				ActionTarget.CODEC.listOf().fieldOf("sequence").forGetter(Sequence::targets)
		).apply(i, Sequence::new));

		public static final Codec<Sequence> INLINE_CODEC = ActionTarget.CODEC.listOf().xmap(Sequence::new, Sequence::targets);

		@Override
		public ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources) {
			for (ActionTarget target : targets) {
				sources = target.resolveTargets(game, sources);
			}
			return sources;
		}

		@Override
		public MapCodec<Sequence> codec() {
			return MAP_CODEC;
		}
	}

	record Excluding(ActionTarget excluding) implements ActionTarget {
		public static final MapCodec<Excluding> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				ActionTarget.CODEC.fieldOf("excluding").forGetter(Excluding::excluding)
		).apply(i, Excluding::new));

		@Override
		public ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources) {
			List<Entity> entities = new ArrayList<>(sources.asEntities(game));
			entities.removeAll(excluding.resolveTargets(game, sources).asEntities(game));
			return ActionSubjects.ofEntities(entities);
		}

		@Override
		public MapCodec<Excluding> codec() {
			return MAP_CODEC;
		}
	}

	record PlayersAround(
			float distance,
			boolean includeSource
	) implements ActionTarget {
		public static final MapCodec<PlayersAround> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.FLOAT.fieldOf("distance").forGetter(PlayersAround::distance),
				Codec.BOOL.optionalFieldOf("include_source", false).forGetter(PlayersAround::includeSource)
		).apply(i, PlayersAround::new));

		@Override
		public ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources) {
			List<Entity> sourceEntities = sources.asEntities(game);
			List<ServerPlayer> targets = new ArrayList<>();
			for (final ServerPlayer otherPlayer : game.participants()) {
				if (sourceEntities.contains(otherPlayer) && !includeSource) {
					continue;
				}
				if (sourceEntities.stream().anyMatch(target -> target.closerThan(otherPlayer, distance))) {
					targets.add(otherPlayer);
				}
			}
			return ActionSubjects.ofPlayers(targets);
		}

		@Override
		public MapCodec<PlayersAround> codec() {
			return MAP_CODEC;
		}
	}

	record FilterEntities(
			EntityPredicate predicate
	) implements ActionTarget {
		public static final MapCodec<FilterEntities> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				EntityPredicate.CODEC.fieldOf("predicate").forGetter(FilterEntities::predicate)
		).apply(i, FilterEntities::new));

		@Override
		public ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources) {
			return ActionSubjects.ofEntities(List.copyOf(Collections2.filter(sources.asEntities(game), entity ->
					!predicate.matches(game.level(), null, entity)
			)));
		}

		@Override
		public MapCodec<FilterEntities> codec() {
			return MAP_CODEC;
		}
	}

	record SpecificPlayer(UUID id) implements ActionTarget {
		public static final MapCodec<SpecificPlayer> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(SpecificPlayer::id)
		).apply(i, SpecificPlayer::new));

		@Override
		public ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources) {
			ServerPlayer player = game.allPlayers().getPlayerBy(id);
			if (player != null) {
				return ActionSubjects.ofPlayer(player);
			}
			return ActionSubjects.EMPTY;
		}

		@Override
		public MapCodec<SpecificPlayer> codec() {
			return MAP_CODEC;
		}
	}

	record SpecificTeam(GameTeamKey team) implements ActionTarget {
		public static final MapCodec<SpecificTeam> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				GameTeamKey.CODEC.fieldOf("team").forGetter(SpecificTeam::team)
		).apply(i, SpecificTeam::new));

		@Override
		public ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources) {
			return ActionSubjects.ofTeam(team);
		}

		@Override
		public MapCodec<SpecificTeam> codec() {
			return MAP_CODEC;
		}
	}
}
