package net.tropicraft.core.common.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.tropicraft.Constants;
import net.tropicraft.core.common.entity.hostile.TropiSkellyEntity;
import net.tropicraft.core.common.entity.neutral.EIHEntity;
import net.tropicraft.core.common.entity.neutral.IguanaEntity;
import net.tropicraft.core.common.entity.neutral.TreeFrogEntity;
import net.tropicraft.core.common.entity.passive.EntityKoaHunter;
import net.tropicraft.core.common.entity.passive.FailgullEntity;
import net.tropicraft.core.common.entity.passive.TropiCreeperEntity;
import net.tropicraft.core.common.entity.placeable.BeachFloatEntity;
import net.tropicraft.core.common.entity.placeable.ChairEntity;
import net.tropicraft.core.common.entity.placeable.UmbrellaEntity;
import net.tropicraft.core.common.entity.placeable.WallItemEntity;
import net.tropicraft.core.common.entity.projectile.LavaBallEntity;
import net.tropicraft.core.common.entity.projectile.PoisonBlotEntity;
import net.tropicraft.core.common.entity.underdasea.MarlinEntity;
import net.tropicraft.core.common.entity.underdasea.SeahorseEntity;
import net.tropicraft.core.common.entity.underdasea.TropicraftDolphinEntity;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Constants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TropicraftEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = new DeferredRegister<>(ForgeRegistries.ENTITIES, Constants.MODID);

    public static final RegistryObject<EntityType<EntityKoaHunter>> KOA_HUNTER = register("koa", TropicraftEntities::koaHunter);
    public static final RegistryObject<EntityType<TropiCreeperEntity>> TROPI_CREEPER = register("tropicreeper", TropicraftEntities::tropicreeper);
    public static final RegistryObject<EntityType<IguanaEntity>> IGUANA = register("iguana", TropicraftEntities::iguana);
    public static final RegistryObject<EntityType<UmbrellaEntity>> UMBRELLA = register("umbrella", TropicraftEntities::umbrella);
    public static final RegistryObject<EntityType<ChairEntity>> CHAIR = register("chair", TropicraftEntities::chair);
    public static final RegistryObject<EntityType<BeachFloatEntity>> BEACH_FLOAT = register("beach_float", TropicraftEntities::beachFloat);
    public static final RegistryObject<EntityType<TropiSkellyEntity>> TROPI_SKELLY = register("tropiskelly", TropicraftEntities::tropiskelly);
    public static final RegistryObject<EntityType<EIHEntity>> EIH = register("eih", TropicraftEntities::eih);
    public static final RegistryObject<EntityType<WallItemEntity>> WALL_ITEM = register("wall_item", TropicraftEntities::wallItem);
    public static final RegistryObject<EntityType<BambooItemFrame>> BAMBOO_ITEM_FRAME = register("bamboo_item_frame", TropicraftEntities::bambooItemFrame);
    // TODO: Register again when volcano eruption is finished
    public static final RegistryObject<EntityType<LavaBallEntity>> LAVA_BALL = null;//register("lava_ball", TropicraftEntities::lavaBall);
    public static final RegistryObject<EntityType<SeaTurtleEntity>> SEA_TURTLE = register("turtle", TropicraftEntities::turtle);
    public static final RegistryObject<EntityType<MarlinEntity>> MARLIN = register("marlin", TropicraftEntities::marlin);
    public static final RegistryObject<EntityType<FailgullEntity>> FAILGULL = register("failgull", TropicraftEntities::failgull);
    public static final RegistryObject<EntityType<TropicraftDolphinEntity>> DOLPHIN = register("dolphin", TropicraftEntities::dolphin);
    public static final RegistryObject<EntityType<SeahorseEntity>> SEAHORSE = register("seahorse", TropicraftEntities::seahorse);
    public static final RegistryObject<EntityType<PoisonBlotEntity>> POISON_BLOT = register("poison_blot", TropicraftEntities::poisonBlot);
    public static final RegistryObject<EntityType<TreeFrogEntity>> TREE_FROG = register("tree_frog", TropicraftEntities::treeFrog);

    private static <E extends Entity, T extends EntityType<E>> RegistryObject<EntityType<E>> register(final String name, final Supplier<EntityType.Builder<E>> sup) {
        return ENTITIES.register(name, () -> sup.get().build(name));
    }

    private static EntityType.Builder<TreeFrogEntity> treeFrog() {
        return EntityType.Builder.create(TreeFrogEntity::new, EntityClassification.CREATURE)
                .size(0.6F, 0.4F)
                .setTrackingRange(80)
                .setUpdateInterval(3)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<PoisonBlotEntity> poisonBlot() {
        return EntityType.Builder.<PoisonBlotEntity>create(PoisonBlotEntity::new, EntityClassification.MISC)
                .size(0.25F, 0.25F)
                .setTrackingRange(32)
                .setUpdateInterval(1)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<SeahorseEntity> seahorse() {
        return EntityType.Builder.create(SeahorseEntity::new, EntityClassification.WATER_CREATURE)
                .size(0.5F, 0.6F)
                .setTrackingRange(80)
                .setUpdateInterval(3)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<TropicraftDolphinEntity> dolphin() {
        return EntityType.Builder.create(TropicraftDolphinEntity::new, EntityClassification.WATER_CREATURE)
                .size(1.4F, 0.5F)
                .setTrackingRange(80)
                .setUpdateInterval(1)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<FailgullEntity> failgull() {
        return EntityType.Builder.create(FailgullEntity::new, EntityClassification.CREATURE)
                .size(0.4F, 0.6F)
                .setTrackingRange(80)
                .setUpdateInterval(3)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<MarlinEntity> marlin() {
        return EntityType.Builder.create(MarlinEntity::new, EntityClassification.WATER_CREATURE)
                .size(1.4F, 0.95F)
                .setTrackingRange(80)
                .setUpdateInterval(1)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<SeaTurtleEntity> turtle() {
        return EntityType.Builder.create(SeaTurtleEntity::new, EntityClassification.WATER_CREATURE)
                .size(0.8F, 0.35F)
                .setTrackingRange(80)
                .setUpdateInterval(1)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<BambooItemFrame> bambooItemFrame() {
        return EntityType.Builder.<BambooItemFrame>create(BambooItemFrame::new, EntityClassification.MISC)
                .size(0.5F, 0.5F)
                .setTrackingRange(64)
                .setUpdateInterval(10)
                .setShouldReceiveVelocityUpdates(false);
    }

    private static EntityType.Builder<LavaBallEntity> lavaBall() {
        return EntityType.Builder.<LavaBallEntity>create(LavaBallEntity::new, EntityClassification.MISC)
                .size(1.0F, 1.0F)
                .setTrackingRange(64)
                .setUpdateInterval(10)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<WallItemEntity> wallItem() {
        return EntityType.Builder.<WallItemEntity>create(WallItemEntity::new, EntityClassification.MISC)
                .size(0.5F, 0.5F)
                .setTrackingRange(64)
                .setUpdateInterval(10)
                .setShouldReceiveVelocityUpdates(false);
    }

    private static EntityType.Builder<EIHEntity> eih() {
        return EntityType.Builder.create(EIHEntity::new, EntityClassification.CREATURE)
                .size(1.2F, 3.25F)
                .setTrackingRange(80)
                .setUpdateInterval(3)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<TropiSkellyEntity> tropiskelly() {
        return EntityType.Builder.create(TropiSkellyEntity::new, EntityClassification.CREATURE)
                .size(0.7F, 1.95F)
                .setTrackingRange(80)
                .setUpdateInterval(3)
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<UmbrellaEntity> umbrella() {
        return EntityType.Builder.<UmbrellaEntity>create(UmbrellaEntity::new, EntityClassification.MISC)
                .size(1.0F, 4.0F)
                .setTrackingRange(120)
                .setUpdateInterval(10)
                .setShouldReceiveVelocityUpdates(false);
    }

    private static EntityType.Builder<ChairEntity> chair() {
        return EntityType.Builder.<ChairEntity>create(ChairEntity::new, EntityClassification.MISC)
                .size(1.5F, 0.5F)
                .setTrackingRange(120)
                .setUpdateInterval(10)
                .setShouldReceiveVelocityUpdates(false);
    }

    private static EntityType.Builder<BeachFloatEntity> beachFloat() {
        return EntityType.Builder.<BeachFloatEntity>create(BeachFloatEntity::new, EntityClassification.MISC)
                .size(2F, 0.175F)
                .setTrackingRange(80)
                .setUpdateInterval(3)
                .setShouldReceiveVelocityUpdates(false);
    }
    
    private static EntityType.Builder<IguanaEntity> iguana() {
        return EntityType.Builder.create(IguanaEntity::new, EntityClassification.CREATURE)
                .size(1.0F, 0.4F)
                .setTrackingRange(80)
                .setUpdateInterval(3)
                .immuneToFire()
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<EntityKoaHunter> koaHunter() {
        return EntityType.Builder.create(EntityKoaHunter::new, EntityClassification.MISC)
                .size(0.6F, 1.95F)
                .setTrackingRange(64)
                .setUpdateInterval(3)
                .immuneToFire()
                .setShouldReceiveVelocityUpdates(true);
    }

    private static EntityType.Builder<TropiCreeperEntity> tropicreeper() {
        return EntityType.Builder.create(TropiCreeperEntity::new, EntityClassification.CREATURE)
                .size(0.6F, 1.7F)
                .setTrackingRange(80)
                .setUpdateInterval(3)
                .setShouldReceiveVelocityUpdates(true);
    }
}
