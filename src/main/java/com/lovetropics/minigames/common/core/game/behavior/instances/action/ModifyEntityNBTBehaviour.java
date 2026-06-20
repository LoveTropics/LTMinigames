package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;

import java.util.UUID;

public record ModifyEntityNBTBehaviour (
		CompoundTag tag
) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<ModifyEntityNBTBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		CompoundTag.CODEC.optionalFieldOf("tag", new CompoundTag()).forGetter(ModifyEntityNBTBehaviour::tag)
	).apply(i, ModifyEntityNBTBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToEntities(game, (context, entity) -> {
			CompoundTag merge = NbtPredicate.getEntityTagToCompare(entity).copy().merge(tag);
			UUID uuid = entity.getUUID();
			try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
				entity.load(TagValueInput.create(problemreporter$scopedcollector, entity.registryAccess(), merge));
				entity.setUUID(uuid);
				return true;
			}
		});
	}
}
