package org.lovetropics.games.common.core.integration.game_actions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.event.GamePackageEvents;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public record DonationPackageGameAction(
		String packageType,
		String sendingPlayerName,
		Optional<UUID> receivingPlayer,
		Optional<GameTeamKey> receivingTeam,
		Optional<Double> amount
) implements GameAction {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<DonationPackageGameAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("package_type").forGetter(c -> c.packageType),
			Codec.STRING.optionalFieldOf("sending_player_name", "").forGetter(c -> c.sendingPlayerName),
			UUIDUtil.STRING_CODEC.optionalFieldOf("receiving_player").forGetter(c -> c.receivingPlayer),
			GameTeamKey.CODEC.optionalFieldOf("receiving_team").forGetter(c -> c.receivingTeam),
			Codec.DOUBLE.optionalFieldOf("amount").forGetter(c -> c.amount)
	).apply(i, DonationPackageGameAction::new));

	@Override
	public boolean apply(IGamePhase game) {
		ActionSubjects<?> specificTargets;
		if (receivingTeam.isPresent()) {
			TeamState teams = game.instanceState().getOrDefault(TeamState.KEY, TeamState.EMPTY);
			GameTeam receivingTeam = teams.getTeamByKey(this.receivingTeam.get());
			if (receivingTeam == null) {
				LOGGER.warn("Could not find team target for package: {}", this);
				return false;
			}
			specificTargets = ActionSubjects.ofTeam(receivingTeam.key());
		} else if (receivingPlayer.isPresent()) {
			ServerPlayer receivingPlayer = game.allPlayers(true).getPlayerBy(this.receivingPlayer.get());
			if (receivingPlayer == null) {
				// Player not on the server or in the game for some reason :(
				LOGGER.warn("Could not find player target for package: {}", this);
				return false;
			}
			specificTargets = ActionSubjects.ofPlayer(receivingPlayer);
		} else {
			specificTargets = null;
		}

		TriState result = game.invoker(GamePackageEvents.RECEIVE_PACKAGE).onReceivePackage(new GamePackage(
				packageType,
				!sendingPlayerName.isEmpty() ? sendingPlayerName : null,
				specificTargets,
				amount
		));
		LOGGER.debug("Handled package action: {} with result {}", this, result);
		return result.isTrue();
	}
}
