package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.function.Supplier;

public class CrabGolfWinBehavior implements PersistentGameBehavior {
	public static final MapCodec<CrabGolfWinBehavior> CODEC = MapCodec.unit(CrabGolfWinBehavior::new);

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(CrabGolfEvents.WIN_GAME, (hole, player, score) -> {
			game.level().playSound(null, player.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.BLOCKS, 0.4F, 1.0F);
		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CRAB_GOLF_WIN;
	}
}
