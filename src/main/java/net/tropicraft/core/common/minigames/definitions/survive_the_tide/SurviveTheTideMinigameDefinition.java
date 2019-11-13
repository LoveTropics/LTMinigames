package net.tropicraft.core.common.minigames.definitions.survive_the_tide;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.*;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants.BlockFlags;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.tropicraft.core.client.data.TropicraftLangKeys;
import net.tropicraft.core.common.Util;
import net.tropicraft.core.common.config.ConfigLT;
import net.tropicraft.core.common.dimension.TropicraftWorldUtils;
import net.tropicraft.core.common.minigames.IMinigameDefinition;
import net.tropicraft.core.common.minigames.IMinigameInstance;
import net.tropicraft.core.common.minigames.MinigameManager;
import weather2.MinigameWeatherInstance;
import weather2.MinigameWeatherInstanceServer;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Definition implementation for the Island Royale minigame.
 *
 * Will resolve minigame features and logic in worldUpdate() method
 * later on.
 */
public class SurviveTheTideMinigameDefinition implements IMinigameDefinition {
    public static ResourceLocation ID = Util.resource("survive_the_tide");
    private String displayName = TropicraftLangKeys.MINIGAME_SURVIVE_THE_TIDE;

    public static final Logger LOGGER = LogManager.getLogger();

    public static boolean debug = true;

    private MinigameWeatherInstanceServer minigameWeatherInstance;

    private MinigamePhase phase = MinigamePhase.PHASE1;

    private long minigameTime = 0;
    private long phaseTime = 0;

    private int waterLevel;

    private BlockPos waterLevelMin = new BlockPos(5722, 0, 6782);
    private BlockPos waterLevelMax = new BlockPos(6102, 0, 7162);

    private MinecraftServer server;

    private boolean minigameEnded;
    private int minigameEndedTimer;
    private UUID winningPlayer;

    public enum MinigamePhase {
        PHASE1,
        PHASE2,
        PHASE3,
        PHASE4,
    }

    public SurviveTheTideMinigameDefinition(MinecraftServer server) {
        this.minigameWeatherInstance = new MinigameWeatherInstanceServer();
        this.server = server;
    }

    @Override
    public ActionResult<ITextComponent> canStartMinigame() {
        ServerWorld world = DimensionManager.getWorld(this.server, this.getDimension(), false, false);

        if (world != null) {
            if (world.getPlayers().size() <= 0) {
                DimensionManager.unloadWorld(world);
                return new ActionResult<>(ActionResultType.FAIL, new StringTextComponent("The Survive the Tide dimension was not unloaded. Begun unloading, please try again in a few seconds.").applyTextStyle(TextFormatting.RED));
            }

            return new ActionResult<>(ActionResultType.FAIL, new StringTextComponent("Cannot start minigame as players are in Survive The Tide dimension. Make them teleport out first.").applyTextStyle(TextFormatting.RED));
        }

        return new ActionResult<>(ActionResultType.SUCCESS, new StringTextComponent(""));
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public String getUnlocalizedName() {
        return this.displayName;
    }

    @Override
    public DimensionType getDimension() {
        return TropicraftWorldUtils.SURVIVE_THE_TIDE_DIMENSION;
    }

    @Override
    public GameType getParticipantGameType() {
        return GameType.ADVENTURE;
    }

    @Override
    public GameType getSpectatorGameType() {
        return GameType.SPECTATOR;
    }

    @Override
    public BlockPos getSpectatorPosition() {
        return ConfigLT.minigame_SurviveTheTide_spectatorPosition;
    }

    @Override
    public BlockPos getPlayerRespawnPosition(IMinigameInstance instance) {
        return ConfigLT.minigame_SurviveTheTide_respawnPosition;
    }

    @Override
    public BlockPos[] getParticipantPositions() {
        return ConfigLT.minigame_SurviveTheTide_playerPositions;
    }

    @Override
    public int getMinimumParticipantCount() {
        return ConfigLT.MINIGAME_SURVIVE_THE_TIDE.minimumPlayerCount.get();
    }

    @Override
    public int getMaximumParticipantCount() {
        return ConfigLT.MINIGAME_SURVIVE_THE_TIDE.maximumPlayerCount.get();
    }

    @Override
    public void worldUpdate(World world, IMinigameInstance instance) {
        if (world.getDimension().getType() == getDimension()) {
            this.checkForGameEndCondition(instance);

            minigameTime++;
            phaseTime++;

            this.processWaterLevel(world);

            if (phase == MinigamePhase.PHASE1) {
                if (phaseTime >= ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase1Length.get()) {
                    nextPhase();

                    for (UUID uuid : instance.getAllPlayerUUIDs()) {
                        ServerPlayerEntity player = this.server.getPlayerList().getPlayerByUUID(uuid);

                        if (player != null) {
                            player.sendMessage(new TranslationTextComponent(TropicraftLangKeys.SURVIVE_THE_TIDE_PVP_ENABLED).applyTextStyle(TextFormatting.RED), ChatType.CHAT);
                        }
                    }
                }
            } else if (phase == MinigamePhase.PHASE2) {
                if (phaseTime >= ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase2Length.get()) {
                    nextPhase();
                }
            } else if (phase == MinigamePhase.PHASE3) {
                if (phaseTime >= ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase3Length.get()) {
                    nextPhase();
                }
            } else if (phase == MinigamePhase.PHASE4) {
                if (this.minigameTime % 100 == 0) {
                    this.growIcebergs(world);
                }
            }

            minigameWeatherInstance.tick(this);
        }
    }

    @Override
    public void onPlayerDeath(ServerPlayerEntity player, IMinigameInstance instance) {
        if (!instance.getSpectators().contains(player.getUniqueID())) {
            instance.removeParticipant(player);
            instance.addSpectator(player);

            player.setGameType(GameType.SPECTATOR);
        }

        if (instance.getParticipants().size() == 1) {
            this.minigameEnded = true;

            this.winningPlayer = instance.getParticipants().iterator().next();
        }
    }

    @Override
    public void onPlayerHurt(LivingHurtEvent event, IMinigameInstance instance) {
        if (event.getSource().getTrueSource() instanceof PlayerEntity && this.phase == MinigamePhase.PHASE1) {
            event.setCanceled(true);
        }
    }

    @Override
    public void onLivingEntityUpdate(LivingEntity entity, IMinigameInstance instance) {
        if (entity.posY <= this.waterLevel + 1 && entity.isInWater() && entity.ticksExisted % 20 == 0) {
            entity.attackEntityFrom(DamageSource.DROWN, 2.0F);
        }
    }

    @Override
    public void onPlayerRespawn(ServerPlayerEntity player, IMinigameInstance instance) {

    }

    @Override
    public void onFinish(CommandSource commandSource, IMinigameInstance instance) {
        this.minigameEnded = false;
        this.minigameEndedTimer = 0;
        this.winningPlayer = null;
        minigameWeatherInstance.reset();
        phase = MinigamePhase.PHASE1;
        phaseTime = 0;
    }

    @Override
    public void onPostFinish(CommandSource commandSource) {
        ServerWorld world = this.server.getWorld(this.getDimension());
        DimensionManager.unloadWorld(world);
    }

    @Override
    public void onPreStart() {
        fetchBaseMap(this.server);
    }

    @Override
    public void onStart(CommandSource commandSource, IMinigameInstance instance) {
        minigameTime = 0;
        ServerWorld world = this.server.getWorld(this.getDimension());
        waterLevel = world.getSeaLevel();
        phase = MinigamePhase.PHASE1;

        for (UUID uuid : instance.getAllPlayerUUIDs()) {
            ServerPlayerEntity player = this.server.getPlayerList().getPlayerByUUID(uuid);

            if (player != null) {
                int minutes = ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase1Length.get() / 20 / 60;
                player.sendMessage(new TranslationTextComponent(TropicraftLangKeys.SURVIVE_THE_TIDE_START).applyTextStyle(TextFormatting.GRAY), ChatType.CHAT);
                player.sendMessage(new TranslationTextComponent(TropicraftLangKeys.SURVIVE_THE_TIDE_PVP_DISABLED, new StringTextComponent(String.valueOf(minutes))).applyTextStyle(TextFormatting.YELLOW), ChatType.CHAT);
            }
        }

        minigameWeatherInstance.setMinigameActive(true);
    }

    public MinigameWeatherInstance getMinigameWeatherInstance() {
        return minigameWeatherInstance;
    }

    public void setMinigameWeatherInstance(MinigameWeatherInstanceServer minigameWeatherInstance) {
        this.minigameWeatherInstance = minigameWeatherInstance;
    }

    public MinigamePhase getPhase() {
        return phase;
    }

    public void setPhase(MinigamePhase phase) {
        this.phase = phase;
    }

    public void nextPhase() {
        if (phase == MinigamePhase.PHASE1) {
            phase = MinigamePhase.PHASE2;
        } else if (phase == MinigamePhase.PHASE2) {
            phase = MinigamePhase.PHASE3;
        } else if (phase == MinigamePhase.PHASE3) {
            phase = MinigamePhase.PHASE4;
        }
        LOGGER.info("Starting minigame phase " + phase);
        phaseTime = 0;
    }

    public long getMinigameTime() {
        return minigameTime;
    }

    public void setMinigameTime(long minigameTime) {
        this.minigameTime = minigameTime;
    }

    public long getPhaseTime() {
        return phaseTime;
    }

    public void setPhaseTime(long phaseTime) {
        this.phaseTime = phaseTime;
    }

    public void dbg(String str) {
        if (debug) {
            LOGGER.info(str);
        }
    }

    private void checkForGameEndCondition(IMinigameInstance instance) {
        if (this.minigameEnded) {
            if (this.minigameEndedTimer == 0) {
                ServerPlayerEntity winning = this.server.getPlayerList().getPlayerByUUID(this.winningPlayer);

                if (winning != null) {
                    for (UUID uuid : instance.getAllPlayerUUIDs()) {
                        ServerPlayerEntity player = this.server.getPlayerList().getPlayerByUUID(uuid);

                        if (player != null) {
                            player.sendMessage(new TranslationTextComponent(TropicraftLangKeys.SURVIVE_THE_TIDE_FINISH, winning.getDisplayName(), ChatType.CHAT));
                            player.sendMessage(new TranslationTextComponent(TropicraftLangKeys.MINIGAME_FINISH), ChatType.CHAT);
                        }
                    }
                }
            }

            this.minigameEndedTimer++;

            if (this.minigameEndedTimer >= 200) {
                MinigameManager.getInstance().finishCurrentMinigame();
            }
        }
    }

    private static void saveMapTo(File from, File to) {
        try {
            if (from.exists()) {
                FileUtils.deleteDirectory(to);

                if (to.mkdirs()) {
                    FileUtils.copyDirectory(from, to);
                }
            } else {
                LOGGER.info("Island royale base map doesn't exist in " + to.getPath() + ", add first before it can copy and replace each game start.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void saveBaseMap(MinecraftServer server) {
        File worldFile = server.getWorld(DimensionType.OVERWORLD).getSaveHandler().getWorldDirectory();

        File baseMapsFile = new File(worldFile, "minigame_base_maps");

        File islandRoyaleBase = new File(baseMapsFile, "hunger_games");
        File islandRoyaleCurrent = new File(worldFile, "tropicraft/hunger_games");

        saveMapTo(islandRoyaleCurrent, islandRoyaleBase);
    }

    public static void fetchBaseMap(MinecraftServer server) {
        File worldFile = server.getWorld(DimensionType.OVERWORLD).getSaveHandler().getWorldDirectory();

        File baseMapsFile = new File(worldFile, "minigame_base_maps");

        File islandRoyaleBase = new File(baseMapsFile, "hunger_games");
        File islandRoyaleCurrent = new File(worldFile, "tropicraft/hunger_games");

        saveMapTo(islandRoyaleBase, islandRoyaleCurrent);
    }

    private void processWaterLevel(World world) {
        if (phase == MinigamePhase.PHASE2 || phase == MinigamePhase.PHASE3) {
            int waterChangeInterval;

            if (phase == MinigamePhase.PHASE2) {
                waterChangeInterval = this.calculateWaterChangeInterval(
                        ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase2TargetWaterLevel.get(),
                        126,
                        ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase2Length.get()
                        );
            }
            else {
                waterChangeInterval = this.calculateWaterChangeInterval(
                        ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase3TargetWaterLevel.get(),
                        ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase2TargetWaterLevel.get(),
                        ConfigLT.MINIGAME_SURVIVE_THE_TIDE.phase3Length.get());
            }

            if (minigameTime % waterChangeInterval == 0) {
                this.waterLevel++;
                BlockPos min = this.waterLevelMin.add(0, this.waterLevel, 0);
                BlockPos max = this.waterLevelMax.add(0, this.waterLevel, 0);
                ChunkPos minChunk = new ChunkPos(min);
                ChunkPos maxChunk = new ChunkPos(max);

                long startTime = System.currentTimeMillis();
                long updatedBlocks = 0;

                MutableBlockPos localStart = new MutableBlockPos();
                MutableBlockPos localEnd = new MutableBlockPos();
                MutableBlockPos realPos = new MutableBlockPos();

                for (int x = minChunk.x; x <= maxChunk.x; x++) {
                    for (int z = minChunk.z; z <= maxChunk.z; z++) {
                        ChunkPos chunkPos = new ChunkPos(x, z);
                        BlockPos chunkStart = chunkPos.asBlockPos();
                        // Extract current chunk section
                        Chunk chunk = world.getChunk(x, z);
                        ChunkSection[] sectionArray = chunk.getSections();
                        ChunkSection section = sectionArray[this.waterLevel >> 4];
                        int localY = this.waterLevel & 0xF;
                        // Calculate start/end within the current section
                        localStart.setPos(min.subtract(chunkStart));
                        localStart.setPos(Math.max(0, localStart.getX()), localY, Math.max(0, localStart.getZ()));
                        localEnd.setPos(max.subtract(chunkStart));
                        localEnd.setPos(Math.min(15, localEnd.getX()), localY, Math.min(15, localEnd.getZ()));
                        // If this section is empty, we must add a new one
                        if (section == Chunk.EMPTY_SECTION) {
                            // This constructor expects the "base y" which means the real Y-level floored to the nearest multiple of 16
                            // This is accomplished by removing the last 4 bits of the coordinate
                            section = new ChunkSection(this.waterLevel & ~0xF);
                            sectionArray[this.waterLevel >> 4] = section;
                        }
                        Heightmap heightmapSurface = chunk.func_217303_b(Type.WORLD_SURFACE);
                        Heightmap heightmapMotionBlocking = chunk.func_217303_b(Type.MOTION_BLOCKING);
                        boolean anyChanged = false;
                        for (BlockPos pos : BlockPos.getAllInBoxMutable(localStart, localEnd)) {
                            BlockState existing = section.getBlockState(pos.getX(), pos.getY(), pos.getZ());
                            realPos.setPos(chunkStart.getX() + pos.getX(), this.waterLevel, chunkStart.getZ() + pos.getZ());
                            BlockState toSet = null;
                            if (existing.isAir(world, pos) || !existing.getMaterial().blocksMovement() || existing.getBlock() == Blocks.BAMBOO) {
                                // If air or a replaceable block, just set to water
                                toSet = Blocks.WATER.getDefaultState();
                            } else if (existing.getBlock() instanceof IWaterLoggable) {
                                // If waterloggable, set the waterloggable property to true
                                toSet = existing.with(BlockStateProperties.WATERLOGGED, true);
                                if (existing.getBlock() == Blocks.CAMPFIRE) {
                                    toSet = toSet.with(CampfireBlock.LIT, false);
                                }
                            }
                            if (toSet != null) {
                                anyChanged = true;
                                if (existing.getBlock() == Blocks.BAMBOO) {
                                    world.setBlockState(realPos, toSet, BlockFlags.NO_RERENDER | BlockFlags.BLOCK_UPDATE);
                                } else {
                                    section.setBlockState(pos.getX(), pos.getY(), pos.getZ(), toSet);
                                }
                                // Tell the client about the change
                                ((ServerChunkProvider)world.getChunkProvider()).markBlockChanged(realPos);
                                // Update heightmap
                                heightmapSurface.update(pos.getX(), realPos.getY(), pos.getZ(), toSet);
                                heightmapMotionBlocking.update(pos.getX(), realPos.getY(), pos.getZ(), toSet);
                                updatedBlocks++;
                                // FIXES LIGHTING AT THE COST OF PERFORMANCE - TODO ask fry?
                                // world.getChunkProvider().getLightManager().checkBlock(realPos);
                            }
                        }
                        if (anyChanged) {
                            // Make sure this chunk gets saved
                            chunk.markDirty();
                        }
                    }
                }

                long endTime = System.currentTimeMillis();
                LogManager.getLogger().info("Updated {} blocks in {}ms", updatedBlocks, endTime - startTime);
            }
        }
    }

    private void growIcebergs(World world) {
        for (IcebergLine line : ConfigLT.minigame_SurviveTheTide_icebergLines) {
            line.generate(world, this.waterLevel);
        }
    }

    private int calculateWaterChangeInterval(int targetLevel, int prevLevel, int phaseLength) {
        int waterLevelDiff = prevLevel - targetLevel;
        return phaseLength / waterLevelDiff;
    }
}