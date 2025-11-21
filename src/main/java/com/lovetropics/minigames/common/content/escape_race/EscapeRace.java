package com.lovetropics.minigames.common.content.escape_race;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.behaviours.BreakBucksBehaviour;
import com.lovetropics.minigames.common.content.escape_race.behaviours.ItemFrameCodeBehaviour;
import com.lovetropics.minigames.common.content.escape_race.behaviours.VacationDaysBehaviour;
import com.lovetropics.minigames.common.content.escape_race.behaviours.TerryBossBehaviour;
import com.lovetropics.minigames.common.content.escape_race.behaviours.WarehouseSetupBehaviour;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.render.DDRMachineEntityRenderer;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrSessionState;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.effect.BasicMobEffect;
import com.lovetropics.minigames.common.content.escape_race.effect.TapirTakeoverMobEffect;
import com.lovetropics.minigames.common.content.escape_race.effect.UpsetStomachEffect;
import com.lovetropics.minigames.common.content.escape_race.misc.RoomEntrancePadEntity;
import com.lovetropics.minigames.common.content.escape_race.misc.RoomEntrancePadEntityRenderer;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntityRenderer;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.GameClientTweakEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CommonColors;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

import static net.minecraft.world.level.storage.loot.LootTable.lootTable;

@EventBusSubscriber(modid = LoveTropics.ID)
public class EscapeRace {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final ResourceKey<Registry<DdrLevel>> DDR_LEVEL = ResourceKey.createRegistryKey(LoveTropics.location("ddr_level"));

	public static final DeferredRegister<EntityDataSerializer<?>> ENTITY_SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS, LoveTropics.ID);
	public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LoveTropics.ID);

	public static final EntityDataSerializer<List<ItemStack>> ITEM_STACK_LIST = new EntityDataSerializer<>() {
		@Override
		public StreamCodec<? super RegistryFriendlyByteBuf, List<ItemStack>> codec() {
			return ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());
		}

		@Override
		public List<ItemStack> copy(List<ItemStack> value) {
			return List.copyOf(Lists.transform(value, ItemStack::copy));
		}
	};
	public static final EntityDataSerializer<DDRMachineEntity.DDRMachineState> DDR_STATE = EntityDataSerializer.forValueType(
			DDRMachineEntity.DDRMachineState.STREAM_CODEC
	);
	public static final EntityDataSerializer<DdrSessionState> DDR_SESSION = EntityDataSerializer.forValueType(DdrSessionState.STREAM_CODEC);

	public static final EntityDataSerializer<DdrInput> DDR_INPUT = EntityDataSerializer.forValueType(DdrInput.STREAM_CODEC);

	public static DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<?>> register(String name, EntityDataSerializer<?> dataSerializerEntry) {
		return ENTITY_SERIALIZERS.register(name, () -> dataSerializerEntry);
	}

	@SubscribeEvent
	public static void registerDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
		event.dataPackRegistry(DDR_LEVEL, DdrLevel.DIRECT_CODEC, DdrLevel.DIRECT_CODEC);
	}

	public static final RegistryEntry<EntityType<?>, EntityType<VendingMachineEntity>> VENDING_MACHINE = REGISTRATE.entity("vending_machine", VendingMachineEntity::new, MobCategory.MISC)
			.properties(properties -> properties.sized(2.0F, 3.0F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.lang("Vending Machine")
			.renderer(() -> VendingMachineEntityRenderer::new)
			.register();

	public static final RegistryEntry<EntityType<?>, EntityType<DDRMachineEntity>> DDR_MACHINE = REGISTRATE.entity("ddr_machine", DDRMachineEntity::new, MobCategory.MISC)
			.properties(properties ->
					properties.sized(3.0F, 3.0F)
							.setShouldReceiveVelocityUpdates(true)
							.clientTrackingRange(8)
							.passengerAttachments(new Vec3(0f, 0.6f, 0.35f))
							.updateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.lang("DDR Machine")
			.renderer(() -> DDRMachineEntityRenderer::new)
			.register();

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VENDING_MACHINE_COST = DATA_COMPONENTS.registerComponentType(
			"vending_machine_cost",
			builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT)
	);
	public static final ItemEntry<Item> BREAK_BUCK = REGISTRATE.item("break_buck", Item::new)
			.lang("Break Buck")
			.register();

	public static final RegistryEntry<EntityType<?>, EntityType<RoomEntrancePadEntity>> ROOM_ENTRANCE_PAD = REGISTRATE.entity("room_entrance_pad", RoomEntrancePadEntity::new, MobCategory.MISC)
			.properties(properties ->
					properties.sized(1F, 1F)
							.setShouldReceiveVelocityUpdates(true)
							.clientTrackingRange(32)
							.updateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.lang("Room Entrance Pad")
			.renderer(() -> RoomEntrancePadEntityRenderer::new)
			.register();

	public static final GameClientTweakEntry<EscapeRaceClientBucksState> BREAK_BUCK_STATE = REGISTRATE.object("break_buck_count")
			.clientState(EscapeRaceClientBucksState.CODEC)
			.register();

	public static final GameBehaviorEntry<BreakBucksBehaviour> BREAK_BUCKS_BEHAVIOUR = REGISTRATE.object("escape_race/break_bucks").behavior(BreakBucksBehaviour.CODEC).register();
	public static final GameBehaviorEntry<VacationDaysBehaviour> VACATION_DAYS_BEHAVIOUR = REGISTRATE.object("escape_race/vacation_days").behavior(VacationDaysBehaviour.CODEC).register();

	public static final GameBehaviorEntry<TerryTrashBehavior> TERRY_TRASH = REGISTRATE.object("terry_trash")
			.behavior(TerryTrashBehavior.CODEC)
			.register();

	public static final GameBehaviorEntry<WarehouseSetupBehaviour> WAREHOUSE_SETUP_BEHAVIOUR = REGISTRATE.object("escape_race/warehouse_setup").behavior(WarehouseSetupBehaviour.CODEC).register();

	public static final GameBehaviorEntry<ItemFrameCodeBehaviour> ITEM_FRAME_CODE = REGISTRATE.object("item_frame_code")
			.behavior(ItemFrameCodeBehaviour.CODEC)
			.register();

	public static final GameBehaviorEntry<TerryBossBehaviour> TERRY_BOSS = REGISTRATE.object("escape_race/terry_boss")
			.behavior(TerryBossBehaviour.CODEC)
			.register();

	public static final Holder<MobEffect> UPSET_STOMACH = REGISTRATE.object("upset_stomach")
			.mobEffect(() -> new UpsetStomachEffect(MobEffectCategory.HARMFUL).addAttributeModifier(
					Attributes.MOVEMENT_SPEED,
					LoveTropics.location("upset_stomach_slowdown"),
					-0.35,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
			))
			.lang("Upset Stomach").register();

	public static final Holder<MobEffect> COLADARAL_DAMAGE = REGISTRATE.object("coladaral_damage")
			.mobEffect(() -> new BasicMobEffect(MobEffectCategory.HARMFUL, CommonColors.WHITE))
			.lang("Colada-ral Damage")
			.register();

	public static final Holder<MobEffect> TAPIR_TAKEOVER = REGISTRATE.object("tapir_takeover")
			.mobEffect(() -> new TapirTakeoverMobEffect(MobEffectCategory.NEUTRAL))
			.register();

	public static void init() {
		register("itemstack_list", ITEM_STACK_LIST);
		register("ddr_state", DDR_STATE);
		register("ddr_input", DDR_INPUT);
		register("ddr_session", DDR_SESSION);
	}
}
