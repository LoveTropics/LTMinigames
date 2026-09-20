package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.client.toast.NotificationStyle;
import org.lovetropics.games.client.toast.ShowNotificationToastMessage;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.neoforge.network.PacketDistributor;

public record NotificationToastAction(Component text, NotificationStyle style) implements IGameBehavior {
	public static final MapCodec<NotificationToastAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ComponentSerialization.CODEC.fieldOf("text").forGetter(NotificationToastAction::text),
			NotificationStyle.MAP_CODEC.forGetter(NotificationToastAction::style)
	).apply(i, NotificationToastAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		ShowNotificationToastMessage message = new ShowNotificationToastMessage(text, style);
		events.applyToPlayers(game, (context, target) -> {
			PacketDistributor.sendToPlayer(target, message);
			return true;
		});
	}
}
