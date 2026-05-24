package com.lovetropics.minigames.common.content.block;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.block.TrashBlock.Attachment;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateBlockModelGenerator;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.minecraft.client.data.models.BlockModelGenerators.*;

public class LoveTropicsBlocks {

	public static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final Map<TrashType, BlockEntry<TrashBlock>> TRASH = Arrays.stream(TrashType.values())
			.collect(Collectors.toMap(Function.identity(), t -> REGISTRATE.block(t.getId(), p -> new TrashBlock(t, p))
					.properties(p -> p.mapColor(MapColor.PLANT).pushReaction(PushReaction.DESTROY).noCollision().offsetType(BlockBehaviour.OffsetType.XZ))
					.blockstate(() -> Models::generateTrashBlock)
					.simpleItem()
					.register()
			));

	public static void init() {
	}

	private static class Models {
		private static void generateTrashBlock(DataGenContext<Block, TrashBlock> ctx, RegistrateBlockModelGenerator prov) {
			MultiVariant variant = plainVariant(ModelLocationUtils.getModelLocation(ctx.get()));
			prov.blockStateOutput.accept(MultiVariantGenerator.dispatch(ctx.get()).with(PropertyDispatch.initial(TrashBlock.ATTACHMENT, BlockStateProperties.HORIZONTAL_FACING)
					.generate((attachment, direction) -> {
						VariantMutator xRot = attachment == Attachment.WALL ? X_ROT_90 : attachment == Attachment.FLOOR ? NOP : X_ROT_180;
						VariantMutator yRot = switch (direction) {
							case NORTH -> Y_ROT_180;
							case EAST -> Y_ROT_270;
							case WEST -> Y_ROT_90;
							default -> NOP;
						};
						return variant.with(xRot).with(yRot);
					})
			));
		}
	}
}
