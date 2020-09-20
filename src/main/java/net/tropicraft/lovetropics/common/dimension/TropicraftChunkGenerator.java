package net.tropicraft.lovetropics.common.dimension;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.gen.NoiseChunkGenerator;
import net.minecraft.world.gen.OctavesNoiseGenerator;
import net.minecraft.world.gen.WorldGenRegion;

public class TropicraftChunkGenerator extends NoiseChunkGenerator<TropicraftGeneratorSettings> {
    private static final float[] BIOME_WEIGHTS = Util.make(new float[25], (weights) -> {
        for(int xw = -2; xw <= 2; ++xw) {
            for(int zw = -2; zw <= 2; ++zw) {
                float weight = 10.0F / MathHelper.sqrt((float)(xw * xw + zw * zw) + 0.2F);
                weights[xw + 2 + (zw + 2) * 5] = weight;
            }
        }
    });

    private final OctavesNoiseGenerator depthNoise;

    public TropicraftChunkGenerator(IWorld world, BiomeProvider biomeProvider, TropicraftGeneratorSettings settings) {
        super(world, biomeProvider, 4, 8, 256, settings, true);
        randomSeed.skip(2620);
        depthNoise = new OctavesNoiseGenerator(randomSeed, 15, 0);
    }

    @Override
    public void decorate(final WorldGenRegion region) {
        int i = region.getMainChunkX();
        int j = region.getMainChunkZ();
        int k = i * 16;
        int l = j * 16;
        BlockPos blockpos = new BlockPos(k, 0, l);
        Biome biome = getBiome(region.getBiomeManager(), blockpos.add(8, 8, 8));
        SharedSeedRandom sharedseedrandom = new SharedSeedRandom();
        long i1 = sharedseedrandom.setDecorationSeed(region.getSeed(), k, l);

        for(GenerationStage.Decoration deco : GenerationStage.Decoration.values()) {
            try {
                biome.decorate(deco, this, region, i1, sharedseedrandom, blockpos);
            } catch (Exception exception) {
                CrashReport crashreport = CrashReport.makeCrashReport(exception, "Biome decoration");
                crashreport.makeCategory("Generation").addDetail("CenterX", i).addDetail("CenterZ", j).addDetail("Step", deco).addDetail("Seed", i1).addDetail("Biome", Registry.BIOME.getKey(biome));
                throw new ReportedException(crashreport);
            }
        }
    }

    // spawn height
    @Override
    public int getGroundHeight() {
        return 128;
    }

    @Override
    public int getSeaLevel() {
        return getGroundHeight() - 1;
    }

    // get depth / scale
    @Override
    protected double[] getBiomeNoiseColumn(int x, int z) {
        double[] lvt_3_1_ = new double[2];
        float lvt_4_1_ = 0.0F;
        float lvt_5_1_ = 0.0F;
        float lvt_6_1_ = 0.0F;
        final int seaLevel = getSeaLevel();
        float lvt_8_1_ = this.biomeProvider.getNoiseBiome(x, seaLevel, z).getDepth();

        for(int lvt_9_1_ = -2; lvt_9_1_ <= 2; ++lvt_9_1_) {
            for(int lvt_10_1_ = -2; lvt_10_1_ <= 2; ++lvt_10_1_) {
                Biome lvt_11_1_ = this.biomeProvider.getNoiseBiome(x + lvt_9_1_, seaLevel, z + lvt_10_1_);
                float lvt_12_1_ = lvt_11_1_.getDepth();
                float lvt_13_1_ = lvt_11_1_.getScale();

                float lvt_14_1_ = BIOME_WEIGHTS[lvt_9_1_ + 2 + (lvt_10_1_ + 2) * 5] / (lvt_12_1_ + 2.0F);
                if (lvt_11_1_.getDepth() > lvt_8_1_) {
                    lvt_14_1_ /= 2.0F;
                }

                lvt_4_1_ += lvt_13_1_ * lvt_14_1_;
                lvt_5_1_ += lvt_12_1_ * lvt_14_1_;
                lvt_6_1_ += lvt_14_1_;
            }
        }

        lvt_4_1_ /= lvt_6_1_;
        lvt_5_1_ /= lvt_6_1_;
        lvt_4_1_ = lvt_4_1_ * 0.9F + 0.1F;
        lvt_5_1_ = (lvt_5_1_ * 4.0F - 1.0F) / 8.0F;
        lvt_3_1_[0] = (double)lvt_5_1_ + getNoiseDepthAt(x, z);
        lvt_3_1_[1] = (double)lvt_4_1_;
        return lvt_3_1_;
    }

    private double getNoiseDepthAt(int noiseX, int noiseZ) {
        double d0 = this.depthNoise.getValue((double)(noiseX * 200), 10.0D, (double)(noiseZ * 200), 1.0D, 0.0D, true) * 65535.0D / 8000.0D;
        if (d0 < 0.0D) {
            d0 = -d0 * 0.3D;
        }

        d0 = d0 * 3.0D - 2.0D;
        if (d0 < 0.0D) {
            d0 = d0 / 28.0D;
        } else {
            if (d0 > 1.0D) {
                d0 = 1.0D;
            }

            d0 = d0 / 40.0D;
        }

        return d0;
    }

    // yoffset
    @Override
    protected double func_222545_a(double depth, double scale, int yy) {
        // The higher this value is, the higher the terrain is!
        final double baseSize = 17D;
        double yOffsets = ((double)yy - (baseSize + depth * baseSize / 8.0D * 4.0D)) * 12.0D * 128.0D / 256.0D / scale;
        if (yOffsets < 0.0D) {
            yOffsets *= 4.0D;
        }

        return yOffsets;
    }

    // populate noise
    @Override
    protected void fillNoiseColumn(double[] doubles, int x, int z) {
        double xzScale = 684.4119873046875D;
        double yScale = 684.4119873046875D;
        double xzOtherScale = 8.555149841308594D;
        double yOtherScale = 4.277574920654297D;

        // Don't make this too high or you'll end up with aether islands!
        final int topSlideMax = 0;
        final int topSlideScale = 3;

        calcNoiseColumn(doubles, x, z, xzScale, yScale, xzOtherScale, yOtherScale, topSlideScale, topSlideMax);
    }

    @Override
    public void makeBase(IWorld worldIn, IChunk chunkIn) {
        super.makeBase(worldIn, chunkIn);

        ChunkPos chunkPos = chunkIn.getPos();
        int j = chunkPos.x;
        int k = chunkPos.z;
    }

    @Override
    public int func_222529_a(int p_222529_1_, int p_222529_2_, Type heightmapType) {
        int height = super.func_222529_a(p_222529_1_, p_222529_2_, heightmapType);
        return height;
    }
}