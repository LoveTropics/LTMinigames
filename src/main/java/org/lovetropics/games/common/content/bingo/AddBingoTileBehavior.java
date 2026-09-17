package org.lovetropics.games.common.content.bingo;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.util.Codecs;
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
			BingoBoard board = game.instanceState().getOrNull(BingoBoard.KEY);
			return board != null && board.addTile(icon.create(), title, reward, trigger) >= 0;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Bingo.ADD_BINGO_TILE;
	}
}
