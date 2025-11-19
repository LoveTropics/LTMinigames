package com.lovetropics.minigames.common.core.game.behavior.action;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
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
		registry.put("limit_entities", LimitEntities.MAP_CODEC);
		registry.put("players_around", PlayersAround.MAP_CODEC);
		registry.put("specific_player", SpecificPlayer.MAP_CODEC);
		registry.put("specific_team", SpecificTeam.MAP_CODEC);
	}

	default ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> sources) {
		return modifyTargets(game, sources, sources);
	}

	ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources);

	MapCodec<? extends ActionTarget> codec();

	enum Simple implements ActionTarget, StringRepresentable {
		NONE("none", (game, targets, sources) -> ActionSubjects.EMPTY),
		SOURCE("source", (game, targets, sources) -> sources),
		ALL_PLAYERS("all_players", (game, targets, sources) -> ActionSubjects.ofPlayers(game.allPlayers())),
		PARTICIPANTS("participants", (game, targets, sources) -> ActionSubjects.ofPlayers(game.participants())),
		SPECTATORS("spectators", (game, targets, sources) -> ActionSubjects.ofPlayers(game.spectators())),
		WIDEN_TO_TEAM("widen_to_team", (game, targets, sources) -> targets.coerceInto(game, ActionSubjectType.TEAM)),
		WIDEN_TO_PLOT("widen_to_plot", (game, targets, sources) -> targets.coerceInto(game, ActionSubjectType.PLOT)),
		ALL_ENTITIES("all_entities", (game, targets, sources) -> ActionSubjects.ofEntities(Lists.newArrayList(game.level().getAllEntities()))),
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
		public ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources) {
			return resolver.resolveTargets(game, targets, sources);
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
			ActionSubjects<?> resolveTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources);
		}
	}

	record Sequence(List<ActionTarget> sequence) implements ActionTarget {
		public static final MapCodec<Sequence> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				ActionTarget.CODEC.listOf().fieldOf("sequence").forGetter(Sequence::sequence)
		).apply(i, Sequence::new));

		public static final Codec<Sequence> INLINE_CODEC = ActionTarget.CODEC.listOf().xmap(Sequence::new, Sequence::sequence);

		@Override
		public ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources) {
			for (ActionTarget modifier : sequence) {
				targets = modifier.modifyTargets(game, targets, sources);
			}
			return targets;
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
		public ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources) {
			List<Entity> entities = new ArrayList<>(targets.asEntities(game));
			entities.removeAll(excluding.modifyTargets(game, targets, sources).asEntities(game));
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
		public ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources) {
			List<Entity> inputEntities = targets.asEntities(game);
			List<ServerPlayer> outputPlayers = new ArrayList<>();
			for (final ServerPlayer otherPlayer : game.participants()) {
				if (inputEntities.contains(otherPlayer) && !includeSource) {
					continue;
				}
				if (inputEntities.stream().anyMatch(inputEntity -> inputEntity.closerThan(otherPlayer, distance))) {
					outputPlayers.add(otherPlayer);
				}
			}
			return ActionSubjects.ofPlayers(outputPlayers);
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
		public ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources) {
			return ActionSubjects.ofEntities(List.copyOf(Collections2.filter(targets.asEntities(game), entity ->
					predicate.matches(game.level(), null, entity)
			)));
		}

		@Override
		public MapCodec<FilterEntities> codec() {
			return MAP_CODEC;
		}
	}

	record LimitEntities(
			int count
	) implements ActionTarget {
		public static final MapCodec<LimitEntities> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.INT.fieldOf("count").forGetter(LimitEntities::count)
		).apply(i, LimitEntities::new));

		@Override
		public ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources) {
			List<Entity> entities = new ArrayList<>(targets.asEntities(game));
			Util.shuffle(entities, game.random());
			return ActionSubjects.ofEntities(List.copyOf(entities.subList(0, Math.min(count, entities.size()))));
		}

		@Override
		public MapCodec<LimitEntities> codec() {
			return MAP_CODEC;
		}
	}

	record SpecificPlayer(UUID id) implements ActionTarget {
		public static final MapCodec<SpecificPlayer> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(SpecificPlayer::id)
		).apply(i, SpecificPlayer::new));

		@Override
		public ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources) {
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
		public ActionSubjects<?> modifyTargets(IGamePhase game, ActionSubjects<?> targets, ActionSubjects<?> sources) {
			return ActionSubjects.ofTeam(team);
		}

		@Override
		public MapCodec<SpecificTeam> codec() {
			return MAP_CODEC;
		}
	}
}
