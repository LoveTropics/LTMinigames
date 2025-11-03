package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.behaviours.BreakBucksBehaviour;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntityRenderer;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevelClient;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevelTick;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntityRenderer;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.GameClientTweakEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.mojang.serialization.Codec;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.level.storage.loot.LootTable.lootTable;

public class EscapeRace {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, LoveTropics.ID);
	public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LoveTropics.ID);

	public static final EntityDataSerializer<NonNullList<ItemStack>> ITEM_STACK_LIST = new EntityDataSerializer<>() {
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, NonNullList<ItemStack>> codec() {
			return ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));
		}

		@Override
		public NonNullList<ItemStack> copy(NonNullList<ItemStack> value) {
			NonNullList<ItemStack> list = NonNullList.create();
			value.forEach((stack) -> list.add(stack.copy()));
			return list;
		}
	};
	public static final EntityDataSerializer<List<DDRMachineLevelClient>> DDR_LEVEL_LIST = new EntityDataSerializer<>() {
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, List<DDRMachineLevelClient>> codec() {
			return DDRMachineLevelClient.STREAM_CODEC.apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));
		}

		@Override
		public List<DDRMachineLevelClient> copy(List<DDRMachineLevelClient> value) {
			NonNullList<DDRMachineLevelClient> list = NonNullList.create();
			list.addAll(value);
			return list;
		}
	};
	public static final EntityDataSerializer<DDRMachineEntity.DDRMachineState> DDR_STATE = EntityDataSerializer.forValueType(
			DDRMachineEntity.DDRMachineState.STREAM_CODEC
	);

	public static final EntityDataSerializer<Map<Integer, DDRMachineLevelTick>> DDR_LEVEL_TICK_MAP = new EntityDataSerializer<>() {
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, Map<Integer, DDRMachineLevelTick>> codec() {
			return ByteBufCodecs.map(HashMap::new, ByteBufCodecs.INT, DDRMachineLevelTick.STREAM_CODEC, 256);
		}

		@Override
		public Map<Integer, DDRMachineLevelTick> copy(Map<Integer, DDRMachineLevelTick> value) {
			return new HashMap<>(value);
		}
	};

	public static DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<?>> register(String name, EntityDataSerializer<?> dataSerializerEntry) {
		return ENTITY_SERIALIZERS.register(name, () -> dataSerializerEntry);
	}
	public static final RegistryEntry<EntityType<?>, EntityType<VendingMachineEntity>> VENDING_MACHINE = REGISTRATE.entity("vending_machine", VendingMachineEntity::new, MobCategory.MISC)
			.properties(properties -> properties.sized(2.0F, 3.0F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.lang("Vending Machine")
			.renderer(() -> VendingMachineEntityRenderer::new)
			.register();

	public static final RegistryEntry<EntityType<?>, EntityType<DDRMachineEntity>> DDR_MACHINE = REGISTRATE.entity("ddr_machine", DDRMachineEntity::new, MobCategory.MISC)
			.properties(properties ->
					properties.sized(2.0F, 3.0F)
							.setShouldReceiveVelocityUpdates(true)
							.clientTrackingRange(8)
							.passengerAttachments(new Vec3(0.0f, 0.8f, 0.0f))
							.updateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.lang("DDR Machine")
			.renderer(() -> DDRMachineEntityRenderer::new)
			.register();

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VENDINGMACHINE_COMPONENT = DATA_COMPONENTS.registerComponentType(
			"vending_machine_cost",
			builder -> builder.persistent(Codec.INT)
	);
	public static final ItemEntry<Item> BREAK_BUCK = REGISTRATE.item("break_buck", Item::new)
			.lang("Break Buck")
			.register();

	public static final GameClientTweakEntry<EscapeRaceClientBucksState> BREAK_BUCK_STATE = REGISTRATE.object("break_buck_count")
			.clientState(EscapeRaceClientBucksState.CODEC)
			.register();
	public static final GameBehaviorEntry<BreakBucksBehaviour> BREAK_BUCKS_BEHAVIOUR = REGISTRATE.object("escape_race/break_bucks").behavior(BreakBucksBehaviour.CODEC).register();


	public static final GameBehaviorEntry<TerryTrashBehavior> TERRY_TRASH = REGISTRATE.object("terry_trash")
			.behavior(TerryTrashBehavior.CODEC)
			.register();

	public static void init() {
		register("itemstack_list", ITEM_STACK_LIST);
		register("ddr_level_list", DDR_LEVEL_LIST);
		register("ddr_state", DDR_STATE);
		register("ddr_level_tick_map", DDR_LEVEL_TICK_MAP);
	}


}
