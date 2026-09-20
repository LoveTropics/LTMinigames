package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.google.common.collect.Lists;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePackageEvents;
import org.lovetropics.games.common.core.game.state.GamePackageState;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public record TriggerEveryPackageBehavior(Set<String> exclude) implements IGameBehavior {
	public static final MapCodec<TriggerEveryPackageBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("exclude").forGetter(TriggerEveryPackageBehavior::exclude)
	).apply(i, TriggerEveryPackageBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		GamePackageState packages = game.state().get(GamePackageState.KEY);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			GamePackage sourcePackage = context.getOptional(GameActionContextKeys.PACKAGE);
			if (sourcePackage == null) {
				return false;
			}

			boolean applied = false;
			for (DonationPackageData donationPackage : packages.packages()) {
				if (exclude.contains(donationPackage.id()) || donationPackage.id().equals(sourcePackage.packageType())) {
					continue;
				}
				applied |= triggerPackage(game, donationPackage, sourcePackage) == TriState.TRUE;
			}

			return applied;
		});
	}

	private static TriState triggerPackage(IGamePhase game, DonationPackageData packageData, GamePackage sourcePackage) {
		Optional<UUID> targetPlayer = Optional.empty();
		Optional<GameTeamKey> targetTeam = Optional.empty();

		if (packageData.targetSelectionMode() == TargetSelectionMode.SPECIFIC) {
			if (packageData.applyToTeam()) {
				TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
				List<GameTeamKey> teamKeys = teams != null ? List.copyOf(teams.getTeamKeys()) : List.of();
				targetTeam = Util.getRandomSafe(teamKeys, game.random());
			} else {
				List<ServerPlayer> participants = Lists.newArrayList(game.participants());
				targetPlayer = Util.getRandomSafe(participants, game.random()).map(Entity::getUUID);
			}
		}

		GamePackage gamePackage = new GamePackage(packageData.id(), sourcePackage.sendingPlayerName(), targetPlayer, targetTeam);
		return game.invoker(GamePackageEvents.RECEIVE_PACKAGE).onReceivePackage(gamePackage);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.TRIGGER_EVERY_PACKAGE;
	}
}
