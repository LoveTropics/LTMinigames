package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import com.mojang.serialization.MapCodec;

import java.util.Optional;

public record SetPlayerRoleAction(Optional<PlayerRole> role) implements IGameBehavior {
	public static final MapCodec<SetPlayerRoleAction> CODEC = PlayerRole.CODEC.optionalFieldOf("role").xmap(SetPlayerRoleAction::new, SetPlayerRoleAction::role);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		PlayerRole roleOrNull = role.orElse(null);
		events.applyToPlayers(game, (context, target) -> {
			if (game.getRoleFor(target) != roleOrNull) {
				game.setPlayerRole(target, roleOrNull);
				target.setHealth(target.getMaxHealth());
				return true;
			}
			return false;
		});
	}
}
