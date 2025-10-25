package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntityRenderer;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

import static net.minecraft.world.level.storage.loot.LootTable.lootTable;

public class EscapeRace {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, LoveTropics.ID);

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
	public static DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<?>> register(String name, EntityDataSerializer<?> dataSerializerEntry) {
		return ENTITY_SERIALIZERS.register(name, () -> dataSerializerEntry);
	}
	public static final RegistryEntry<EntityType<?>, EntityType<VendingMachineEntity>> VENDING_MACHINE = REGISTRATE.entity("vending_machine", VendingMachineEntity::new, MobCategory.MISC)
			.properties(properties -> properties.sized(2.0F, 3.0F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.lang("Vending Machine")
			.renderer(() -> VendingMachineEntityRenderer::new)
			.register();


	public static void init() {
		register("itemstack_list", ITEM_STACK_LIST);
	}
}
