package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
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

public record GiveDimensionTransportItemAction(
		ItemStackTemplate itemStackTemplate,
		Holder<DimensionType> targetDimension,
		RegionQuery targetRegion
) implements IGameBehavior {
	private static final DeferredHolder<DataComponentType<?>, DataComponentType<?>> TRANSPORT_DATA_COMPONENT = DeferredHolder.create(Registries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath("ltextras", "transport_to_dimension"));
	public static final MapCodec<GiveDimensionTransportItemAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(GiveDimensionTransportItemAction::itemStackTemplate),
			DimensionType.CODEC.fieldOf("target_dimension_type").forGetter(GiveDimensionTransportItemAction::targetDimension),
			RegionQuery.CODEC.fieldOf("target_region").forGetter(GiveDimensionTransportItemAction::targetRegion)
	).apply(instance, GiveDimensionTransportItemAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			ResourceKey<Level> vanillaKey = dimensionTypeToVanillaKey(targetDimension);
			if(vanillaKey == null){
				return false;
			}
			targets.asPlayers(game).forEach(player -> {
				ItemStack itemStack = itemStackTemplate.create();
				ResourceKey<Level> targetLevel = LinkedDimensions.resolve(player.level(), vanillaKey);
				Region targetRegion = this.targetRegion.oneIn(game, game.server().getLevel(targetLevel));
				RegistryOps<Tag> ops = game.allLevels().getFirst().registryAccess().createSerializationContext(NbtOps.INSTANCE);
				CompoundTag compoundTag = new CompoundTag();
				compoundTag.putString("destination_level", targetLevel.identifier().toString());
				Tag pos = Vec3.CODEC.encodeStart(ops, targetRegion.center()).getOrThrow();
				compoundTag.put("destination_pos", pos);
				DataResult<?> parse = TRANSPORT_DATA_COMPONENT.get().codecOrThrow().parse(ops, compoundTag);
				TypedDataComponent<?> unchecked = TypedDataComponent.createUnchecked(TRANSPORT_DATA_COMPONENT.get(), parse.getOrThrow());
				itemStack.set(unchecked);
				player.addItem(itemStack);
			});
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
