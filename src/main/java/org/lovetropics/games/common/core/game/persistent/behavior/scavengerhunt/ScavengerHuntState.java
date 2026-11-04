package org.lovetropics.games.common.core.game.persistent.behavior.scavengerhunt;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.data.LoveTropicsAttachments;

import java.util.Collection;

@EventBusSubscriber(modid = LoveTropics.ID)
public class ScavengerHuntState {
	public static final MapCodec<ScavengerHuntState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("next_stage_index").forGetter(s -> s.nextStageIndex)
	).apply(i, ScavengerHuntState::new));

	private int nextStageIndex;

	private ScavengerHuntState(int nextStageIndex) {
		this.nextStageIndex = nextStageIndex;
	}

	public ScavengerHuntState() {
		this(0);
	}

	public void incrementStageIndex() {
		nextStageIndex++;
	}

	public void setNextStageIndex(int index) {
		nextStageIndex = index;
	}

	public int nextStageIndex() {
		return nextStageIndex;
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("persistentgame")
				.then(Commands.literal("scavengerhunt")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("set")
								.then(Commands.argument("players", EntityArgument.players())
										.then(Commands.argument("nextStage", IntegerArgumentType.integer(0))
												.executes(context -> {
													Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
													int nextStage = IntegerArgumentType.getInteger(context, "nextStage");
													for (ServerPlayer player : players) {
														player.getData(LoveTropicsAttachments.SCAVENGER_HUNT_STATE).setNextStageIndex(nextStage);
													}
													context.getSource().sendSuccess(() -> Component.translatable("Set next stage for %s players", players.size()), true);
													return 1;
												})
										)
								)
						)
						.then(Commands.literal("get")
								.then(Commands.argument("player", EntityArgument.player())
										.executes(context -> {
											ServerPlayer player = EntityArgument.getPlayer(context, "player");
											ScavengerHuntState state = player.getData(LoveTropicsAttachments.SCAVENGER_HUNT_STATE);
											context.getSource().sendSuccess(() -> Component.translatable("%s's next stage index is: %s", player.getDisplayName(), state.nextStageIndex), false);
											return 1;
										})
								)
						)
				)
		);
	}
}
