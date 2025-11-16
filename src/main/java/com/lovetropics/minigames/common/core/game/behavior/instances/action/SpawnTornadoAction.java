package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.fml.InterModComms;

public record SpawnTornadoAction(boolean sharknado) implements IGameBehavior {
	public static final MapCodec<SpawnTornadoAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.fieldOf("sharknado").forGetter(c -> c.sharknado)
	).apply(i, SpawnTornadoAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> spawnTornado(game));
	}

	private boolean spawnTornado(IGamePhase game) {

		InterModComms.sendTo("weather2", sharknado ? "sharknado" : "tornado", () -> {
			CompoundTag tag = new CompoundTag();
			tag.putString("dimension", game.level().dimension().location().toString());
			return tag;
		});

		return true;
	}
}
