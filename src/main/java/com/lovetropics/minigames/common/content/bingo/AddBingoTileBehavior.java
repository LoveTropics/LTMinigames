package com.lovetropics.minigames.common.content.bingo;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.util.Codecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.function.Supplier;

public record AddBingoTileBehavior(ItemStackTemplate icon, Component title, int reward, Supplier<GameActionList> trigger) implements IGameBehavior {
	public static final MapCodec<AddBingoTileBehavior> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(AddBingoTileBehavior::icon),
			ComponentSerialization.CODEC.fieldOf("title").forGetter(AddBingoTileBehavior::title),
			Codec.INT.fieldOf("reward").forGetter(AddBingoTileBehavior::reward),
			Codecs.newLazyCopies(GameActionList.CODEC).fieldOf("trigger").forGetter(AddBingoTileBehavior::trigger)
	).apply(in, AddBingoTileBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			var tileIndex = game.invoker(Bingo.REQUEST_NEW_TILE_EVENT).requestTile(icon.create(), title, reward);
			if (tileIndex >= 0) {
				// This event capturing dance allows us to pass a parameter to the actions in the trigger so that those actions can report back when they complete the tile
				var captureListeners = new GameEventListeners();

				// Create new instances of the trigger and register its events
				trigger.get().register(game, events.redirect(t -> t == Bingo.CAPTURE_TILE_EVENT, captureListeners));
				captureListeners.invoker(Bingo.CAPTURE_TILE_EVENT).capture(tileIndex);
				return true;
			}
			return false;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Bingo.ADD_BINGO_TILE;
	}
}
