package org.lovetropics.games.common.core.game.persistent.behavior.scavengerhunt;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.data.LoveTropicsAttachments;

import java.util.Collection;

@EventBusSubscriber(modid = LoveTropics.ID)
public class ScavengerHuntState {
	private static final Codec<Object2IntOpenHashMap<Identifier>> ID_TO_INT_CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.INT).xmap(Object2IntOpenHashMap::new, m -> m);

	public static final MapCodec<ScavengerHuntState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ID_TO_INT_CODEC.fieldOf("next_stage_indices").forGetter(s -> s.nextStageIndices)
	).apply(i, ScavengerHuntState::new));

	private final Object2IntOpenHashMap<Identifier> nextStageIndices;

	private ScavengerHuntState(Object2IntOpenHashMap<Identifier> nextStageIndices) {
		this.nextStageIndices = nextStageIndices;
	}

	public ScavengerHuntState() {
		this(new Object2IntOpenHashMap<>());
	}

	public void incrementStageIndex(Identifier id) {
		nextStageIndices.addTo(id, 1);
	}

	public void setNextStageIndices(Identifier id, int index) {
		nextStageIndices.put(id, index);
	}

	public int nextStageIndex(Identifier id) {
		return nextStageIndices.getInt(id);
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("persistentgame")
				.then(Commands.literal("scavengerhunt")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("id", IdentifierArgument.id())
								.then(Commands.literal("set")
										.then(Commands.argument("players", EntityArgument.players())
												.then(Commands.argument("nextStage", IntegerArgumentType.integer(0))
														.executes(context -> {
															Identifier id = IdentifierArgument.getId(context, "id");
															Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "players");
															int nextStage = IntegerArgumentType.getInteger(context, "nextStage");
															for (ServerPlayer player : players) {
																player.getData(LoveTropicsAttachments.SCAVENGER_HUNT_STATE).setNextStageIndices(id, nextStage);
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
													Identifier id = IdentifierArgument.getId(context, "id");
													ServerPlayer player = EntityArgument.getPlayer(context, "player");
													ScavengerHuntState state = player.getData(LoveTropicsAttachments.SCAVENGER_HUNT_STATE);
													context.getSource().sendSuccess(() -> Component.translatable("%s's next stage index is: %s", player.getDisplayName(), state.nextStageIndices.getInt(id)), false);
													return 1;
												})
										)
								)
						)
				)
		);
	}
}
