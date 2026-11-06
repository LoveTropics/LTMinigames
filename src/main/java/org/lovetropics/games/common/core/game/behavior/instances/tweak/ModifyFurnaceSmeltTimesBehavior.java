package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;

public record ModifyFurnaceSmeltTimesBehavior(float modifier) implements IGameBehavior {
	public static final MapCodec<ModifyFurnaceSmeltTimesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.FLOAT.fieldOf("modifier").forGetter(ModifyFurnaceSmeltTimesBehavior::modifier)
	).apply(i, ModifyFurnaceSmeltTimesBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.COOK_TIME_RESOLVE, (level, originalTime, itemStack) -> (int) (originalTime * modifier));
	}
}
