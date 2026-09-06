package com.lovetropics.minigames.common.core.game.behavior.instances.donation;

import com.lovetropics.minigames.client.toast.NotificationIcon;
import com.lovetropics.minigames.client.toast.NotificationStyle;
import com.lovetropics.minigames.client.toast.ShowNotificationToastMessage;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public record DonationPackageNotification(
		TemplatedText message,
		NotificationIcon icon,
		NotificationStyle.Sentiment sentiment,
		Holder<SoundEvent> sound
) {
	public static final Codec<DonationPackageNotification> CODEC = RecordCodecBuilder.create(i -> i.group(
			TemplatedText.CODEC.fieldOf("message").forGetter(c -> c.message),
			NotificationIcon.CODEC.optionalFieldOf("icon", NotificationIcon.item(new ItemStackTemplate(Items.GRASS_BLOCK))).forGetter(c -> c.icon),
			NotificationStyle.Sentiment.CODEC.optionalFieldOf("sentiment", NotificationStyle.Sentiment.NEUTRAL).forGetter(c -> c.sentiment),
			SoundEvent.CODEC.optionalFieldOf("sound", Holder.direct(SoundEvents.TOTEM_USE)).forGetter(c -> c.sound)
	).apply(i, DonationPackageNotification::new));

	public void onPlayerReceive(final IGamePhase game, @Nullable final ServerPlayer receiver, @Nullable final String sender, final Component packageName) {
		Component targetedMessage = createTargetedMessage(sender, getReceiverName(receiver));
		Component globalMessage = createGlobalMessage(packageName, getReceiverName(receiver));

		for (ServerPlayer player : game.allPlayers()) {
			boolean targeted = player == receiver || receiver == null;
			sendNotificationTo(player, targeted, targeted ? targetedMessage : globalMessage);
		}
	}

	public void onTeamReceive(final IGamePhase game, @Nullable final GameTeam receiver, @Nullable final String sender, final Component packageName) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);

		Component targetedMessage = createTargetedMessage(sender, getReceiverName(receiver));
		Component globalMessage = createGlobalMessage(packageName, getReceiverName(receiver));

		for (ServerPlayer player : game.allPlayers()) {
			boolean targeted = receiver == null || teams != null && teams.isOnTeam(player, receiver.key());
			sendNotificationTo(player, targeted, targeted ? targetedMessage : globalMessage);
		}
	}

	private void sendNotificationTo(ServerPlayer player, boolean targeted, Component message) {
		NotificationStyle.Color color = targeted ? NotificationStyle.Color.LIGHT : NotificationStyle.Color.DARK;
		long visibleTime = targeted ? 8000 : 6000;

		NotificationStyle style = createStyle(color, visibleTime);
		PacketDistributor.sendToPlayer(
				player,
				new ShowNotificationToastMessage(message, style)
		);

		if (targeted) {
			long seed = player.getRandom().nextLong();
			player.connection.send(new ClientboundSoundPacket(sound(), SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 0.2f, 1f, seed));
		}
	}

	private Component createTargetedMessage(@Nullable String sender, Component receiverName) {
		return message.apply(Map.of("sender", getSenderName(sender), "receiver", receiverName));
	}

	private Component createGlobalMessage(Component packageName, Component receiverName) {
		return MinigameTexts.PACKAGE_RECEIVED.apply(receiverName, packageName.copy().withStyle(sentiment.textStyle()));
	}

	public NotificationStyle createStyle(final NotificationStyle.Color color, final long visibleTime) {
		return new NotificationStyle(icon, sentiment, color, visibleTime);
	}

	private Component getReceiverName(@Nullable ServerPlayer receiver) {
		if (receiver != null) {
			return receiver.getDisplayName().copy().withStyle(ChatFormatting.BLUE);
		}
		return MinigameTexts.EVERYONE_RECEIVER;
	}

	private Component getReceiverName(@Nullable GameTeam receiver) {
		if (receiver != null) {
			return receiver.config().styledName();
		}
		return MinigameTexts.EVERYONE_RECEIVER;
	}

	private Component getSenderName(@Nullable String sender) {
		if (sender != null) {
			return Component.literal(sender).withStyle(ChatFormatting.BLUE);
		}
		return MinigameTexts.UNKNOWN_DONOR;
	}
}
