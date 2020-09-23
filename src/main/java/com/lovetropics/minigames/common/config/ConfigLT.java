package com.lovetropics.minigames.common.config;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.common.minigames.behaviours.instances.survive_the_tide.IcebergLine;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

@EventBusSubscriber
public class ConfigLT {

    private static final Builder CLIENT_BUILDER = new Builder();

    private static final Builder COMMON_BUILDER = new Builder();

    public static final CategoryGeneral GENERAL = new CategoryGeneral();

    // TODO: Remove as we're using new minigame data driven configs instead
    public static final CategorySurviveTheTide MINIGAME_SURVIVE_THE_TIDE = new CategorySurviveTheTide();
    
    public static final CategoryTiltify TILTIFY = new CategoryTiltify();
    
//    public static final CategoryBiomes BIOMES = new CategoryBiomes();

    public static final class CategorySurviveTheTide {

        public final IntValue minimumPlayerCount;
        public final IntValue maximumPlayerCount;

        public final ConfigValue<String> minigame_SurviveTheTide_playerPositions;
        public final ConfigValue<String> minigame_SurviveTheTide_respawnPosition;
        public final ConfigValue<String> minigame_SurviveTheTide_spectatorPosition;
        public final ConfigValue<String> minigame_SurviveTheTide_spawnAreaP1;
        public final ConfigValue<String> minigame_SurviveTheTide_spawnAreaP2;
        public final ConfigValue<String> icebergLines;

        public final IntValue phase0Length;
        public final IntValue phase1Length;
        public final IntValue phase2Length;
        public final IntValue phase3Length;

        public final IntValue phase2TargetWaterLevel;
        public final IntValue phase3TargetWaterLevel;

        public final DoubleValue rainHeavyChance;
        public final DoubleValue rainAcidChance;
        public final DoubleValue heatwaveChance;

        public final IntValue rainHeavyMinTime;
        public final IntValue rainHeavyExtraRandTime;
        public final IntValue rainAcidMinTime;
        public final IntValue rainAcidExtraRandTime;
        public final IntValue heatwaveMinTime;
        public final IntValue heatwaveExtraRandTime;

        public final DoubleValue heatwaveMovementMultiplier;

        public final IntValue acidRainDamage;
        public final IntValue acidRainDamageRate;

        public final BooleanValue minigame_SurviveTheTide_worlderBorderEnabled;
        public final IntValue minigame_SurviveTheTide_worldBorder_ticksAfterPhase4;
        public final IntValue minigame_SurviveTheTide_worldBorder_ticksUtilFullyShrinked;

        public final IntValue minigame_SurviveTheTide_worldBorder_particleRateDelay;
        public final IntValue minigame_SurviveTheTide_worldBorder_particleHeight;

        public final IntValue minigame_SurviveTheTide_worldBorder_damageRateDelay;
        public final IntValue minigame_SurviveTheTide_worldBorder_damageAmount;

        public final BooleanValue minigame_SurviveTheTide_worlderBorder_acidRainOnPhase4;

        //TODO: PARTICLE RATE, PARTICLE HEIGHT? DAMAGE RATE
        //public final IntValue minigame_SurviveTheTide_ticksUtilWorlderBorderFullyShrinked;



        private CategorySurviveTheTide() {
            COMMON_BUILDER.comment("Survive The Tide settings").push("survive_the_tide");

            minigame_SurviveTheTide_playerPositions = COMMON_BUILDER.comment("List of spawn positions for players, number of entries must match maximumPlayerCount config value, separate each position by ; and each x y and z with , example: 5780, 141, 6955; 5780, 141, 6955")
                    .define("minigame_SurviveTheTide_playerPositions", "5906,133,6962;5910,133,6961;5914,133,6961;5918,133,6962;5922,133,6966;" +
                            "5923,133,6970;5923,133,6974;5922,133,6978;5918,133,6982;5914,133,6983;5910,133,6983;5906,133,6982;5902,133,6978;5901,133,6974;" +
                            "5901,133,6970;5902,133,6966;");

            icebergLines = COMMON_BUILDER.comment("List of iceberg lines, tuples of block positions.")
                    .define("icebergLines", "5964,164,6879;5826,167,6906]5807,167,6924;5840,167,7050]5865,167,7068;5983,169,7054]5968,173,6907;5859,170,7047]" +
                            "5894,173,6995;5894,173,6965]5894,173,6965;5923,173,6960]5925,173,6962;5928,173,6991]5897,174,7000;5932,174,6997]5892,185,6963;5930,166,6999]" +
                            "6011,168,7036;5987,168,6917]");

            minigame_SurviveTheTide_spawnAreaP1 = COMMON_BUILDER.define("minigame_SurviveTheTide_spawnAreaP1", "5895, 133, 6954");
            minigame_SurviveTheTide_spawnAreaP2 = COMMON_BUILDER.define("minigame_SurviveTheTide_spawnAreaP2", "5928, 133, 6989");

            minigame_SurviveTheTide_respawnPosition = COMMON_BUILDER.define("minigame_SurviveTheTide_respawnPosition", "5780, 141, 6955");
            minigame_SurviveTheTide_spectatorPosition = COMMON_BUILDER.define("minigame_SurviveTheTide_spectatorPosition", "5780, 141, 6955");

            minimumPlayerCount = COMMON_BUILDER.defineInRange("minimumPlayerCount", 3, 1, 255);
            maximumPlayerCount = COMMON_BUILDER.defineInRange("maximumPlayerCount", 16, 2, 255);

            phase0Length = COMMON_BUILDER.comment("Time in ticks pre game phase will last").defineInRange("phase0Length", 20*30, 1, Integer.MAX_VALUE);
            phase1Length = COMMON_BUILDER.comment("Time in ticks first game phase will last").defineInRange("phase1Length", 20*60*2, 1, Integer.MAX_VALUE);
            phase2Length = COMMON_BUILDER.comment("Time in ticks second game phase will last").defineInRange("phase2Length", 20*60*6, 1, Integer.MAX_VALUE);
            phase3Length = COMMON_BUILDER.comment("Time in ticks third game phase will last").defineInRange("phase3Length", 20*60*4, 1, Integer.MAX_VALUE);

            phase2TargetWaterLevel = COMMON_BUILDER.comment("Target water level for second game phase").defineInRange("phase2TargetWaterLevel", 133, 1, Integer.MAX_VALUE);
            phase3TargetWaterLevel = COMMON_BUILDER.comment("Target water level for third game phase").defineInRange("phase3TargetWaterLevel", 150, 1, Integer.MAX_VALUE);

            rainHeavyChance = COMMON_BUILDER.comment("Tried every second, 0.01 = 1% chance, 1 = 100% chance").defineInRange("rainHeavyChance", 0.01, 0, 1D);
            rainAcidChance = COMMON_BUILDER.comment("Tried every second, 0.01 = 1% chance, 1 = 100% chance").defineInRange("rainAcidChance", 0.01, 0, 1D);
            heatwaveChance = COMMON_BUILDER.comment("Tried every second, 0.01 = 1% chance, 1 = 100% chance").defineInRange("heatwaveChance", 0.01, 0, 1D);

            rainHeavyMinTime = COMMON_BUILDER.defineInRange("rainHeavyMinTime", 20*60*1, 1, Integer.MAX_VALUE);
            rainHeavyExtraRandTime = COMMON_BUILDER.defineInRange("rainHeavyExtraRandTime", 20*60*1, 1, Integer.MAX_VALUE);
            rainAcidMinTime = COMMON_BUILDER.defineInRange("rainAcidMinTime", 20*60*1, 1, Integer.MAX_VALUE);
            rainAcidExtraRandTime = COMMON_BUILDER.defineInRange("rainAcidExtraRandTime", 20*60*1, 1, Integer.MAX_VALUE);
            heatwaveMinTime = COMMON_BUILDER.defineInRange("heatwaveMinTime", 20*60*1, 1, Integer.MAX_VALUE);
            heatwaveExtraRandTime = COMMON_BUILDER.defineInRange("heatwaveExtraRandTime", 20*60*1, 1, Integer.MAX_VALUE);

            heatwaveMovementMultiplier = COMMON_BUILDER.defineInRange("heatwaveMovementMultiplier", 0.5, 0.01, 1D);

            acidRainDamage = COMMON_BUILDER.defineInRange("acidRainDamage", 1, 1, Integer.MAX_VALUE);
            acidRainDamageRate = COMMON_BUILDER.comment("Rate in ticks, 20 = 1 second").defineInRange("acidRainDamageRate", 60, 1, Integer.MAX_VALUE);

            minigame_SurviveTheTide_worlderBorderEnabled = COMMON_BUILDER.define("minigame_SurviveTheTide_worlderBorderEnabled", true);

            minigame_SurviveTheTide_worldBorder_ticksAfterPhase4 = COMMON_BUILDER.defineInRange("minigame_SurviveTheTide_worldBorder_ticksAfterPhase4", 20*60*5, 1, Integer.MAX_VALUE);
            minigame_SurviveTheTide_worldBorder_ticksUtilFullyShrinked = COMMON_BUILDER.defineInRange("minigame_SurviveTheTide_worldBorder_ticksUtilFullyShrinked", 20*60*5, 1, Integer.MAX_VALUE);

            minigame_SurviveTheTide_worldBorder_particleRateDelay = COMMON_BUILDER.defineInRange("minigame_SurviveTheTide_worldBorder_particleRateDelay", 1, 1, Integer.MAX_VALUE);
            minigame_SurviveTheTide_worldBorder_particleHeight = COMMON_BUILDER.defineInRange("minigame_SurviveTheTide_worldBorder_particleHeight", 110, 1, Integer.MAX_VALUE);
            minigame_SurviveTheTide_worldBorder_damageRateDelay = COMMON_BUILDER.defineInRange("minigame_SurviveTheTide_worldBorder_damageRateDelay", 20, 1, Integer.MAX_VALUE);
            minigame_SurviveTheTide_worldBorder_damageAmount = COMMON_BUILDER.defineInRange("minigame_SurviveTheTide_worldBorder_damageAmount", 4, 1, Integer.MAX_VALUE);

            minigame_SurviveTheTide_worlderBorder_acidRainOnPhase4 = COMMON_BUILDER.define("minigame_SurviveTheTide_worlderBorder_acidRainOnPhase4", true);

            COMMON_BUILDER.pop();
        }
    }

    public static final class CategoryGeneral {

        public final DoubleValue Precipitation_Particle_effect_rate;

        public final BooleanValue UseCrouch;

        private CategoryGeneral() {
            CLIENT_BUILDER.comment("General mod settings").push("general");

            Precipitation_Particle_effect_rate = CLIENT_BUILDER
                    .defineInRange("Precipitation_Particle_effect_rate", 0.7D, 0D, 1D);

            UseCrouch = CLIENT_BUILDER.comment("Enable crawling anywhere by pressing the sprint key while holding down the sneak key")
                    .define("UseCrawl", true);

            CLIENT_BUILDER.pop();
        }
    }
    
    public static final class CategoryTiltify {
        
        public final ConfigValue<String> appToken;
        public final IntValue campaignId;
        public final IntValue donationTrackerRefreshRate;
        public final IntValue donationAmountPerMonument;
        public final ConfigValue<String> tiltifyCommandRun;
        
        private CategoryTiltify() {
            COMMON_BUILDER.comment("Used for the LoveTropics charity drive.").push("tiltify");
            
            appToken = COMMON_BUILDER
                    .comment("Add a token here to enable donation tracking, leave blank to disable")
                    .define("tiltifyAppToken", "");
            campaignId = COMMON_BUILDER
                    .comment("The tiltify campaign to track donations from")
                    .defineInRange("tiltifyCampaign", 0, 0, 99999999);
            donationTrackerRefreshRate = COMMON_BUILDER
                    .comment("How often the tracker checks for new donations, in seconds")
                    .defineInRange("donationTrackerRefreshRate", 10, 1, 1000);
            donationAmountPerMonument = COMMON_BUILDER
                    .comment("Amount of $ required per monument command run")
                    .defineInRange("donationAmountPerMonument", 500, 1, 100000);
            tiltifyCommandRun = COMMON_BUILDER
                    .comment("Command run when donation comes in")
                    .define("tiltifyCOmmandRun", "function internaluseonly:addmonument");
            
            COMMON_BUILDER.pop();
        }
    }
    
    public static final class CategoryBiomes {
        
        public final IntValue surviveTheTideSkyColor;
        public final IntValue surviveTheTideFoliageColor;
        public final IntValue surviveTheTideGrassColor;
        
        private CategoryBiomes() {
            CLIENT_BUILDER.comment("Biome color settings.").push("biomes");
            
            surviveTheTideSkyColor = CLIENT_BUILDER
                    .comment("The color for the Survive The Tide biome's sky. Can be given in hex code in the format 0xRRGGBB.")
                    .defineInRange("surviveTheTideSkyColor", 0x0f331b, 0, 0xFFFFFF);
            surviveTheTideFoliageColor = CLIENT_BUILDER
                    .comment("The color for the Survive The Tide biome's foliage. Can be given in hex code in the format 0xRRGGBB.")
                    .defineInRange("surviveTheTideFoliageColor", 0x5e8c64, 0, 0xFFFFFF);
            surviveTheTideGrassColor = CLIENT_BUILDER
                    .comment("The color for the Survive The Tide biome's grass. Can be given in hex code in the format 0xRRGGBB.")
                    .defineInRange("surviveTheTideGrassColor", 0x498551, 0, 0xFFFFFF);
            
            CLIENT_BUILDER.pop();
        }
    }

    public static final ForgeConfigSpec CLIENT_CONFIG = CLIENT_BUILDER.build();

    public static final ForgeConfigSpec SERVER_CONFIG = COMMON_BUILDER.build();

    /**
     * values used during runtime that require processing from disk
     */
    public static BlockPos[] minigame_SurviveTheTide_playerPositions = new BlockPos[] {
            new BlockPos(5780, 141, 6955) };

    public static BlockPos minigame_SurviveTheTide_respawnPosition = new BlockPos(5780, 141, 6955);

    public static BlockPos minigame_SurviveTheTide_spectatorPosition = new BlockPos(5780, 141, 6955);

    public static BlockPos minigame_SurviveTheTide_spawnAreaP1 = BlockPos.ZERO;
    public static BlockPos minigame_SurviveTheTide_spawnAreaP2 = BlockPos.ZERO;

    public static List<IcebergLine> minigame_SurviveTheTide_icebergLines = Lists.newArrayList();

    public static void onLoad(final ModConfig.Loading configEvent) {
        minigame_SurviveTheTide_playerPositions = getAsBlockPosArray(ConfigLT.MINIGAME_SURVIVE_THE_TIDE.minigame_SurviveTheTide_playerPositions.get());
        minigame_SurviveTheTide_respawnPosition = stringToBlockPos(ConfigLT.MINIGAME_SURVIVE_THE_TIDE.minigame_SurviveTheTide_respawnPosition.get());
        minigame_SurviveTheTide_spectatorPosition = stringToBlockPos(ConfigLT.MINIGAME_SURVIVE_THE_TIDE.minigame_SurviveTheTide_spectatorPosition.get());
        minigame_SurviveTheTide_icebergLines = getIcebergLinesFromString(ConfigLT.MINIGAME_SURVIVE_THE_TIDE.icebergLines.get());
        minigame_SurviveTheTide_spawnAreaP1 = stringToBlockPos(ConfigLT.MINIGAME_SURVIVE_THE_TIDE.minigame_SurviveTheTide_spawnAreaP1.get());
        minigame_SurviveTheTide_spawnAreaP2 = stringToBlockPos(ConfigLT.MINIGAME_SURVIVE_THE_TIDE.minigame_SurviveTheTide_spawnAreaP2.get());

        //for (BlockPos pos : minigame_SurviveTheTide_playerPositions) System.out.println("RESULT: " + pos);
    }

    public static void onFileChange(final ModConfig.Reloading configEvent) {
        //System.out.println("file changed!" + configEvent.toString());
    }

    public static String blockPosString(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static String blockPositionsString(BlockPos[] pos) {
        String result = "";
        for (BlockPos p : pos) {
            result = result.concat(blockPosString(p) + ";");
        }

        return result;
    }

    public static String icebergLineString(BlockPos start, BlockPos end) {
        return blockPosString(start) + ";" + blockPosString(end) + "]";
    }

    public static BlockPos stringToBlockPos(String posString) {
        try {
            String splitterInt = ",";
            String[] substr = posString.split(splitterInt);
            return new BlockPos(
                    Integer.valueOf(substr[0].trim()),
                    Integer.valueOf(substr[1].trim()),
                    Integer.valueOf(substr[2].trim()));
        } catch (Exception ex) {
            //in case of bad config, at least dont tp them out of world
            return BlockPos.ZERO.add(0, 255, 0);
        }

    }

    public static BlockPos[] getAsBlockPosArray(String string) {
        String splitterBlockPos = ";";
        String[] posStrings = string.split(splitterBlockPos);
        List<BlockPos> listPos = new ArrayList<>();

        for (String posString : posStrings) {
            try {
                listPos.add(stringToBlockPos(posString));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return listPos.stream().toArray(BlockPos[]::new);
    }

    public static List<IcebergLine> getIcebergLinesFromString(String string) {
        String splitBlockArrays = "]";

        String[] blockPosArrayStrings = string.split(splitBlockArrays);
        List<IcebergLine> listArrays = Lists.newArrayList();

        for (String arrayString : blockPosArrayStrings) {
            try {
                BlockPos[] array = getAsBlockPosArray(arrayString);
                listArrays.add(new IcebergLine(array[0], array[1], 10));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return listArrays;
    }
}
