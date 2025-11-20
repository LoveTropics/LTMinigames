package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Supplier;

public record WarehouseSetupBehaviour(
		List<RoomConfig> rooms
) implements IGameBehavior {
	public static final MapCodec<WarehouseSetupBehaviour> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(RoomConfig.CODEC.listOf().fieldOf("rooms").forGetter(WarehouseSetupBehaviour::rooms)).apply(inst, WarehouseSetupBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		GameWidgets widgets = GameWidgets.getOrRegister(game, events);

		Warehouse warehouse = new Warehouse(game, teams, widgets, rooms);

		events.listen(GamePhaseEvents.TICK, warehouse::tick);

		events.listen(GamePlayerEvents.ADD, player ->
				// Fade back in if returning from a room
				PlayerSet.of(player).fadeFromBlack(Warehouse.FADE_DURATION)
		);

		events.listen(GamePlayerEvents.JOIN, warehouse::onPlayerJoin);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				warehouse.registerCommands(game, commands)
		);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return EscapeRace.WAREHOUSE_SETUP_BEHAVIOUR;
	}

	public record RoomConfig(
			String entranceRegion,
			float facing,
			int baseCost,
			Component displayName,
			ResourceLocation gameId
	) {
		public static final Codec<RoomConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("entrance_region").forGetter(RoomConfig::entranceRegion),
				Codec.FLOAT.fieldOf("facing").forGetter(RoomConfig::facing),
				Codec.INT.fieldOf("cost").forGetter(RoomConfig::baseCost),
				ComponentSerialization.CODEC.fieldOf("display_name").forGetter(RoomConfig::displayName),
				ResourceLocation.CODEC.fieldOf("game").forGetter(RoomConfig::gameId)
		).apply(i, RoomConfig::new));
	}
}
