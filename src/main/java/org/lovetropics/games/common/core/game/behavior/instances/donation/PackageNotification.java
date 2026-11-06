package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.lovetropics.lib.text.ChatHighlighter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lovetropics.games.SoundRegistry;
import org.lovetropics.games.client.toast.NotificationIcon;
import org.lovetropics.games.client.toast.NotificationStyle;
import org.lovetropics.games.client.toast.ShowNotificationToastMessage;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjectDescriber;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.util.TemplatedText;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;

import java.util.Map;
import java.util.Optional;

public record PackageNotification(
		Optional<TemplatedText> message,
		NotificationIcon icon,
		Optional<NotificationStyle.Sentiment> sentiment,
		Optional<Holder<SoundEvent>> sound
) {
	public static final Codec<PackageNotification> CODEC = RecordCodecBuilder.create(i -> i.group(
			TemplatedText.CODEC.optionalFieldOf("message").forGetter(c -> c.message),
			NotificationIcon.CODEC.optionalFieldOf("icon", NotificationIcon.item(new ItemStackTemplate(Items.GRASS_BLOCK))).forGetter(c -> c.icon),
			NotificationStyle.Sentiment.CODEC.optionalFieldOf("sentiment").forGetter(c -> c.sentiment),
			SoundEvent.CODEC.optionalFieldOf("sound").forGetter(c -> c.sound)
	).apply(i, PackageNotification::new));

	public void broadcast(IGamePhase game, ActionSubjects<?> targets, GamePackage gamePackage, DonationPackageDesc packageDesc) {
		NotificationStyle.Sentiment sentiment = this.sentiment.orElseGet(() -> packageDesc.type().defaultSentiment());
		sendToastToTargets(game, targets, gamePackage, sentiment);
		broadcastChat(game, targets, gamePackage, packageDesc, sentiment);
	}

	private void sendToastToTargets(IGamePhase game, ActionSubjects<?> targets, GamePackage gamePackage, NotificationStyle.Sentiment sentiment) {
		Holder<SoundEvent> sound = this.sound.orElseGet(() -> switch (sentiment) {
			case POSITIVE -> SoundRegistry.PACKAGE_RECEIVE;
			case NEGATIVE, NEUTRAL -> SoundRegistry.SABOTAGE_RECEIVE;
		});
		TemplatedText message = this.message.orElseGet(() ->
				new TemplatedText(Component.translatable("ltminigames.minigame.donation." + gamePackage.packageType() + ".toast")
						.withStyle(switch (sentiment) {
							case POSITIVE, NEUTRAL -> ChatFormatting.LIGHT_PURPLE;
							case NEGATIVE -> ChatFormatting.RED;
						})
				)
		);

		Component targetDescription = ActionSubjectDescriber.describe(game, targets, ChatFormatting.BLUE);
		String senderName = gamePackage.sendingPlayerName();
		Component targetedMessage = message.apply(Map.of(
				"sender", (senderName != null ? Component.literal(senderName) : MinigameTexts.ANONYMOUS_DONOR.copy()).withStyle(ChatFormatting.BLUE),
				"receiver", targetDescription != null ? targetDescription : MinigameTexts.EVERYONE_RECEIVER.copy().withStyle(ChatFormatting.BLUE)
		));

		for (ServerPlayer player : targets.asPlayers(game)) {
			sendToastToTarget(player, targetedMessage, sentiment, sound);
		}
	}

	private void sendToastToTarget(ServerPlayer player, Component message, NotificationStyle.Sentiment sentiment, Holder<SoundEvent> sound) {
		long visibleTime = 6000;

		NotificationStyle style = createStyle(NotificationStyle.Color.LIGHT, visibleTime, sentiment);
		PacketDistributor.sendToPlayer(
				player,
				new ShowNotificationToastMessage(message, style)
		);

		long seed = player.getRandom().nextLong();
		player.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 0.2f, 1f, seed));
	}

	private void broadcastChat(IGamePhase game, ActionSubjects<?> targets, GamePackage gamePackage, DonationPackageDesc packageDesc, NotificationStyle.Sentiment sentiment) {
		Component packageName = packageDesc.name().copy().withStyle(sentiment == NotificationStyle.Sentiment.NEGATIVE ? ChatFormatting.RED : ChatFormatting.GOLD);

		String senderName = gamePackage.sendingPlayerName();
		Component sender = (senderName != null ? Component.literal(senderName) : MinigameTexts.ANONYMOUS_DONOR.copy()).withStyle(ChatFormatting.AQUA);

		Component targetDescription = ActionSubjectDescriber.describe(game, targets, ChatFormatting.AQUA);
		Component message;
		if (targetDescription != null) {
			message = MinigameTexts.PACKAGE_TRIGGERED_WITH_TARGET.apply(sender, packageName, targetDescription);
		} else {
			message = MinigameTexts.PACKAGE_TRIGGERED.apply(sender, packageName);
		}
		game.allPlayers().sendMessage(ChatHighlighter.highlight(
				message,
				0xa0ffaa00,
				0x60519e45
		));
	}

	public NotificationStyle createStyle(NotificationStyle.Color color, long visibleTime, NotificationStyle.Sentiment sentiment) {
		return new NotificationStyle(icon, sentiment, color, visibleTime);
	}
}
