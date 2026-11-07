package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lovetropics.dimensions.LinkedDimensions;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;

import java.util.Optional;

public record GiveCompassWithLodestoneTargetAction(
		ItemStackTemplate itemStackTemplate,
		RegionQuery targetLocation,
		Holder<DimensionType> targetDimension
) implements IGameBehavior {

	public static final MapCodec<GiveCompassWithLodestoneTargetAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(GiveCompassWithLodestoneTargetAction::itemStackTemplate),
			RegionQuery.CODEC.fieldOf("target_region").forGetter(GiveCompassWithLodestoneTargetAction::targetLocation),
			DimensionType.CODEC.fieldOf("target_dimension_type").forGetter(GiveCompassWithLodestoneTargetAction::targetDimension)
	).apply(instance, GiveCompassWithLodestoneTargetAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			ResourceKey<Level> vanillaKey = dimensionTypeToVanillaKey(targetDimension);
			if(vanillaKey == null){
				return false;
			}
			for (ServerPlayer player : targets.asPlayers(game)) {
				ItemStack itemStack = itemStackTemplate.create();
				ResourceKey<Level> targetLevel = LinkedDimensions.resolve(player.level(), vanillaKey);
				Region targetRegion = this.targetLocation.oneIn(game, game.server().getLevel(targetLevel));
				LodestoneTracker lodestoneTracker = new LodestoneTracker(Optional.of(GlobalPos.of(targetLevel, targetRegion.centerBlock())), true);
				itemStack.set(DataComponents.LODESTONE_TRACKER, lodestoneTracker);
				player.addItem(itemStack);
			}
			return true;
		});
	}

	private static @Nullable ResourceKey<Level> dimensionTypeToVanillaKey(Holder<DimensionType> dimensionTypeHolder){
		if(dimensionTypeHolder.is(BuiltinDimensionTypes.OVERWORLD)){
			return Level.OVERWORLD;
		} else if(dimensionTypeHolder.is(BuiltinDimensionTypes.NETHER)){
			return Level.NETHER;
		} else if(dimensionTypeHolder.is(BuiltinDimensionTypes.END)){
			return Level.END;
		}
		return null;
	}
}
