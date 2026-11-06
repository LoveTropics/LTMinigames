package org.lovetropics.games.common.core.game.behavior.instances.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.server.command.EnumArgument;
import org.jetbrains.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.concurrent.atomic.AtomicReference;

public record AnnouncementsBehavior() implements IGameBehavior {
	public static final MapCodec<AnnouncementsBehavior> CODEC = MapCodec.unit(AnnouncementsBehavior::new);
	public enum AnnouncementStyle {
		NEGATIVE(TextColor.fromRgb(0xCC3A4D)),
		NEUTRAL(TextColor.fromRgb(0x31A2AC)),
		POSITIVE(TextColor.fromRgb(0x57B538));
		private final TextColor color;
		AnnouncementStyle(TextColor color){
			this.color = color;
		}

		public TextColor getColor() {
			return color;
		}
	}
	private static final TextColor SUBTITLE_COLOR = TextColor.fromRgb(0xA9B4B7);
	private static final Component EMPTY_TITLE = CommonComponents.space();
	private static final int STAY = SharedConstants.TICKS_PER_SECOND * 2;
	private static final int FADE_IN = SharedConstants.TICKS_PER_SECOND / 4;
	private static final int FADE_OUT = SharedConstants.TICKS_PER_SECOND  / 4;
	private static final FontDescription.Resource MINECRAFT_5 = new FontDescription.Resource(Identifier.fromNamespaceAndPath("lt", "minecraft_5"));
	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			commands.register(Commands.literal("announce")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("target", StringArgumentType.word()).suggests((context, builder) -> {
						builder.suggest("all");
						TeamState orNull = game.instanceState().getOrNull(TeamState.KEY);
						if(orNull != null){
							for (GameTeamKey teamKey : orNull.getTeamKeys()) {
								builder.suggest(teamKey.id());
							}
						}
						return builder.buildFuture();
					})
							.then(Commands.argument("title", StringArgumentType.string())
									.then(Commands.argument("subtitle", StringArgumentType.string())
											.executes(context -> {
												sendAnnouncement(game, StringArgumentType.getString(context, "target"),
														StringArgumentType.getString(context, "title"),
														StringArgumentType.getString(context, "subtitle"));
												return 1;
											})
											.then(Commands.argument("style", EnumArgument.enumArgument(AnnouncementStyle.class))
													.executes(context -> {
														sendAnnouncement(game, StringArgumentType.getString(context, "target"),
																StringArgumentType.getString(context, "title"),
																StringArgumentType.getString(context, "subtitle"),
																context.getArgument("style", AnnouncementStyle.class));
														return 1;
													})))))
			);
		});
	}

	private static void sendAnnouncement(IGamePhase game, String target, String title, String subtitle) {
		sendAnnouncement(game, target, title, subtitle, AnnouncementStyle.NEUTRAL);
	}

	private static void sendAnnouncement(IGamePhase game, String targetString, String title, String subtitle, AnnouncementStyle style) {
		AtomicReference<@Nullable PlayerSet> players = new AtomicReference<>();
		if(targetString.equals("all")){
			players.set(game.participants());
		} else {
			game.instanceState().getOptional(TeamState.KEY).ifPresent(teamState -> {
				GameTeam teamByKey = teamState.getTeamByKey(targetString);
				if(teamByKey != null){
					players.set(teamState.getPlayersForTeam(game, teamByKey.key()));
				}
			});
		}
		PlayerSet targetPlayers = players.get();
		if(targetPlayers == null){
			return;
		}
		Component TITLE = title.isEmpty() ? EMPTY_TITLE : Component.literal(title)
				.withStyle(Style.EMPTY
						.withBold(true)
						.withColor(style.getColor())
						.withFont(MINECRAFT_5));
		Component SUBTITLE = subtitle.isEmpty() ?  EMPTY_TITLE : Component.literal(subtitle)
				.withStyle(Style.EMPTY
						.withColor(SUBTITLE_COLOR)
						.withFont(FontDescription.DEFAULT));
		for (var target : targetPlayers) {
			target.connection.send(new ClientboundClearTitlesPacket(true));
			target.connection.send(new ClientboundSetTitlesAnimationPacket(FADE_IN, STAY, FADE_OUT));
			target.connection.send(new ClientboundSetTitleTextPacket(TITLE));
			if (SUBTITLE != EMPTY_TITLE) {
				target.connection.send(new ClientboundSetSubtitleTextPacket(SUBTITLE));
			}
			target.sendSystemMessage(
					Component.empty()
							.append(TITLE)
							.append(Component.literal(" - ").withColor(TextColor.DARK_GRAY))
							.append(SUBTITLE)
			);
		}
	}

}
