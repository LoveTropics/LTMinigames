package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.InterModComms;

public record TransformPlayerTornadoAction(int timeTicks, boolean baby) implements IGameBehavior {
	public static final MapCodec<TransformPlayerTornadoAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("time_ticks").forGetter(c -> c.timeTicks),
			Codec.BOOL.fieldOf("baby").forGetter(c -> c.baby)
	).apply(i, TransformPlayerTornadoAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, player) -> transformPlayer(player));
	}

	private boolean transformPlayer(final ServerPlayer player) {

		InterModComms.sendTo("weather2", "player_tornado", () -> {
			CompoundTag tag = new CompoundTag();
			tag.putString("uuid", player.getUUID().toString());
			tag.putInt("time_ticks", timeTicks);
			tag.putBoolean("baby", baby);
			tag.putString("dimension", player.level().dimension().identifier().toString());
			return tag;
		});

		return true;
	}
}
