package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.SoundRegistry;
import org.lovetropics.games.client.toast.NotificationIcon;
import org.lovetropics.games.client.toast.NotificationStyle;
import org.lovetropics.games.client.toast.ShowNotificationToastMessage;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.core.game.util.TemplatedText;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record NotifyPackageAction(
		Optional<TemplatedText> message,
		NotificationIcon icon,
		Optional<NotificationStyle.Sentiment> sentiment,
		Optional<Holder<SoundEvent>> sound
) implements IGameBehavior {
	public static final MapCodec<NotifyPackageAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TemplatedText.CODEC.optionalFieldOf("message").forGetter(c -> c.message),
			NotificationIcon.CODEC.optionalFieldOf("icon", NotificationIcon.item(new ItemStackTemplate(Items.GRASS_BLOCK))).forGetter(c -> c.icon),
			NotificationStyle.Sentiment.CODEC.optionalFieldOf("sentiment").forGetter(c -> c.sentiment),
			SoundEvent.CODEC.optionalFieldOf("sound").forGetter(c -> c.sound)
	).apply(i, NotifyPackageAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			GamePackage gamePackage = context.getOptional(GameActionContextKeys.PACKAGE);
			DonationPackageDesc packageDesc = context.getOptional(GameActionContextKeys.PACKAGE_DESC);
			if (gamePackage == null || packageDesc == null) {
				return false;
			}
			broadcast(game, targets, gamePackage, packageDesc);
			return true;
		});
	}

	private void broadcast(IGamePhase game, ActionSubjects<?> targets, GamePackage gamePackage, DonationPackageDesc packageDesc) {
		NotificationStyle.Sentiment sentiment = this.sentiment.orElseGet(() -> packageDesc.type().defaultSentiment());
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

		Component receiverName = describeTargets(game, targets);
		Component targetedMessage = createTargetedMessage(gamePackage.sendingPlayerName(), receiverName, message);
		Component globalMessage = createGlobalMessage(packageDesc.name(), receiverName, sentiment);

		List<ServerPlayer> targetPlayers = targets.asPlayers(game);
		for (ServerPlayer player : game.allPlayers()) {
			boolean targeted = targetPlayers.contains(player);
			sendNotificationTo(player, targeted, targeted ? targetedMessage : globalMessage, sentiment, sound);
		}
	}

	private void sendNotificationTo(ServerPlayer player, boolean targeted, Component message, NotificationStyle.Sentiment sentiment, Holder<SoundEvent> sound) {
		NotificationStyle.Color color = targeted ? NotificationStyle.Color.LIGHT : NotificationStyle.Color.DARK;
		long visibleTime = targeted ? 8000 : 6000;

		NotificationStyle style = createStyle(color, visibleTime, sentiment);
		PacketDistributor.sendToPlayer(
				player,
				new ShowNotificationToastMessage(message, style)
		);

		if (targeted) {
			long seed = player.getRandom().nextLong();
			player.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 0.2f, 1f, seed));
		}
	}

	private Component createTargetedMessage(@Nullable String sender, Component receiverName, TemplatedText template) {
		return template.apply(Map.of("sender", getSenderName(sender), "receiver", receiverName));
	}

	private Component createGlobalMessage(Component packageName, Component receiverName, NotificationStyle.Sentiment sentiment) {
		return MinigameTexts.PACKAGE_RECEIVED.apply(receiverName, packageName.copy().withStyle(sentiment.textStyle()));
	}

	public NotificationStyle createStyle(NotificationStyle.Color color, long visibleTime, NotificationStyle.Sentiment sentiment) {
		return new NotificationStyle(icon, sentiment, color, visibleTime);
	}

	private Component describeTargets(IGamePhase game, ActionSubjects<?> targets) {
		List<ServerPlayer> targetPlayers = targets.asPlayers(game);
		if (Set.copyOf(targetPlayers).containsAll(Sets.newHashSet(game.participants()))) {
			return MinigameTexts.EVERYONE_RECEIVER;
		}

		// Try to figure out the intended target of the package, if we can find teams that fully match this target list, that is likely most relevant
		// Intentionally not using a strong dependency on the package configuration to keep this flexible
		Set<GameTeam> targetTeams = teamsExactlyMatching(game, targetPlayers);
		if (targetTeams != null) {
			return ComponentUtils.formatList(targetTeams, GameTeam::styledName);
		}

		return ComponentUtils.formatList(targetPlayers, p -> p.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
	}

	private static @Nullable Set<GameTeam> teamsExactlyMatching(IGamePhase game, List<ServerPlayer> players) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (teams == null) {
			return null;
		}
		Set<GameTeam> exactTeams = new ReferenceArraySet<>();
		for (ServerPlayer player : players) {
			GameTeamKey key = teams.getTeamForPlayer(player);
			GameTeam team = key != null ? teams.getTeamByKey(key) : null;
			if (team == null) {
				return null;
			}
			if (teams.getParticipantsForTeam(game, key).stream().anyMatch(otherPlayer -> !players.contains(otherPlayer))) {
				// There are other players in this team, but not all of them are included
				return null;
			}
			exactTeams.add(team);
		}
		return exactTeams;
	}

	private Component getSenderName(@Nullable String sender) {
		if (sender != null) {
			return Component.literal(sender).withStyle(ChatFormatting.BLUE);
		}
		return MinigameTexts.UNKNOWN_DONOR;
	}
}
