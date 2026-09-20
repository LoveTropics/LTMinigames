package org.lovetropics.games.common.content.survive_the_tide.behavior;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;

public record SttChatBroadcastBehavior(String downToTwoTranslationKey) implements IGameBehavior {
	public static final MapCodec<SttChatBroadcastBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("down_to_two_translation_key").forGetter(c -> c.downToTwoTranslationKey)
	).apply(i, SttChatBroadcastBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (lastRole != PlayerRole.PARTICIPANT) {
				return;
			}

			PlayerSet participants = game.participants();

			if (participants.size() == 2) {
				Iterator<ServerPlayer> it = participants.iterator();
				ServerPlayer p1 = it.next();
				ServerPlayer p2 = it.next();

				if (p1 != null && p2 != null) {
					Component p1text = p1.getDisplayName().copy().withStyle(ChatFormatting.AQUA);
					Component p2text = p2.getDisplayName().copy().withStyle(ChatFormatting.AQUA);

					game.allPlayers().sendMessage(Component.translatable(downToTwoTranslationKey, p1text, p2text).withStyle(ChatFormatting.GOLD));
				}
			}
		});
	}
}
