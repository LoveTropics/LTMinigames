package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
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
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.util.Util;

import java.util.Optional;

public record GiveDimensionTransportItemAction(
		ItemStackTemplate itemStackTemplate,
		Holder<DimensionType> targetDimension,
		RegionQuery targetRegion,
		Optional<String> subcommand
) implements IGameBehavior {
	private static final DeferredHolder<DataComponentType<?>, DataComponentType<?>> TRANSPORT_DATA_COMPONENT = DeferredHolder.create(Registries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath("ltextras", "transport_to_dimension"));
	public static final MapCodec<GiveDimensionTransportItemAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(GiveDimensionTransportItemAction::itemStackTemplate),
			DimensionType.CODEC.fieldOf("target_dimension_type").forGetter(GiveDimensionTransportItemAction::targetDimension),
			RegionQuery.CODEC.fieldOf("target_region").forGetter(GiveDimensionTransportItemAction::targetRegion),
			Codec.STRING.optionalFieldOf("subcommand").forGetter(GiveDimensionTransportItemAction::subcommand)
	).apply(instance, GiveDimensionTransportItemAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		ResourceKey<Level> vanillaKey = dimensionTypeToVanillaKey(targetDimension);
		if (vanillaKey == null) {
			throw new GameException(Component.literal("Invalid dimension " + targetDimension));
		}
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			targets.asPlayers(game).forEach(player -> giveItemTo(game, player, vanillaKey));
			return true;
		});

		subcommand.ifPresent(command -> events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, _) ->
				commands.register(Commands.literal("givedimensionteleport")
						.then(Commands.literal(command)
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(context -> {
											ServerPlayer player = EntityArgument.getPlayer(context, "player");
											giveItemTo(game, player, vanillaKey);
											return 1;
										})
								)
						)
				)
		));
	}

	private void giveItemTo(IGamePhase game, ServerPlayer player, ResourceKey<Level> vanillaKey) {
		ItemStack itemStack = itemStackTemplate.create();
		ResourceKey<Level> targetLevel = LinkedDimensions.resolve(player.level(), vanillaKey);
		Region targetRegion = this.targetRegion.oneIn(game, game.server().getLevel(targetLevel));
		RegistryOps<Tag> ops = game.allLevels().getFirst().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		CompoundTag compoundTag = new CompoundTag();
		compoundTag.putString("destination_level", targetLevel.identifier().toString());
		Tag pos = Vec3.CODEC.encodeStart(ops, targetRegion.center()).getOrThrow();
		compoundTag.put("destination_pos", pos);
		DataResult<?> parse = TRANSPORT_DATA_COMPONENT.get().codecOrThrow().parse(ops, compoundTag);
		itemStack.set(TypedDataComponent.createUnchecked(TRANSPORT_DATA_COMPONENT.get(), parse.getOrThrow()));
		Util.addItemStackToInventory(player, itemStack);
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
