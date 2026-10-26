package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;

public record NotifyPackageAction(
		Component name,
		DonationPackageNotification notification
) implements IGameBehavior {
	public static final MapCodec<NotifyPackageAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ComponentSerialization.CODEC.fieldOf("name").forGetter(NotifyPackageAction::name),
			DonationPackageNotification.MAP_CODEC.forGetter(NotifyPackageAction::notification)
	).apply(i, NotifyPackageAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			GamePackage gamePackage = context.getOptional(GameActionContextKeys.PACKAGE);
			if (gamePackage == null) {
				return false;
			}
			notification.broadcast(game, targets, gamePackage.sendingPlayerName(), name);
			return true;
		});
	}
}
