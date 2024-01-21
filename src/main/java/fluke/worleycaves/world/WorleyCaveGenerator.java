package fluke.worleycaves.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fluids.IFluidBlock;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import fluke.worleycaves.Main;
import fluke.worleycaves.config.Configs;
import fluke.worleycaves.util.BitArray2D;
import fluke.worleycaves.util.FastNoise;
import fluke.worleycaves.util.FloatArray3D;
import fluke.worleycaves.util.WorleyUtil;

public class WorleyCaveGenerator extends MapGenCaves {
    // int numLogChunks = 500;
    // long[] genTime = new long[numLogChunks];
    // int currentTimeIndex = 0;
    // double sum = 0;

    private WorleyUtil worleyF1divF3 = new WorleyUtil();
    private FastNoise displacementNoisePerlin = new FastNoise();
    private MapGenBase replacementCaves;
    private MapGenBase moddedCaveGen;

    private static Block lava;
    private static int maxCaveHeight;
    private static int minCaveHeight;
    private static float noiseCutoff;
    private static float warpAmplifier;
    private static float easeInDepth;
    private static float yCompression;
    private static float xzCompression;
    private static float surfaceCutoff;
    private static int lavaDepth;
    private static boolean additionalWaterChecks;

    public WorleyCaveGenerator() {
        worleyF1divF3.setFrequency(0.016f);
        displacementNoisePerlin.setFrequency(0.05f);

        maxCaveHeight = Configs.maxCaveHeight;
        minCaveHeight = Configs.minCaveHeight;
        noiseCutoff = (float) Configs.noiseCutoffValue;
        warpAmplifier = (float) Configs.warpAmplifier;
        easeInDepth = (float) Configs.easeInDepth;
        yCompression = (float) Configs.verticalCompressionMultiplier;
        xzCompression = (float) Configs.horizonalCompressionMultiplier;
        surfaceCutoff = (float) Configs.surfaceCutoffValue;
        lavaDepth = Configs.lavaDepth;
        additionalWaterChecks = false; // Loader.isModLoaded("subterranaenwaters");

        lava = Block.getBlockFromName(Configs.lavaBlock);
        if (lava == null) {
            Main.LOGGER.error("Cannot find block " + Configs.lavaBlock);
            lava = Blocks.air;
        }

        // try and grab other modded cave gens, like swiss cheese caves or Quark big caves
        // our replace cavegen event will ignore cave events when the original cave class passed in is a Worley cave
        moddedCaveGen = TerrainGen.getModdedMapGen(this, InitMapGenEvent.EventType.CAVE);
        if (moddedCaveGen != this) replacementCaves = moddedCaveGen;
        else replacementCaves = new MapGenCaves(); // default to vanilla caves if there are no other modded cave gens
    }

    private void debugValueAdjustments() {
        // lavaDepth = 10;
        // noiseCutoff = 0.18F;
        // warpAmplifier = 8.0F;
        // easeInDepth = 15;
        // xzCompression = 0.5f;
    }

    @Override
    public void func_151539_a(IChunkProvider p_151539_1_, World worldIn, int x, int z, Block[] blocks) {
        int currentDim = worldIn.provider.dimensionId;
        this.worldObj = worldIn;
        // revert to vanilla cave generation for blacklisted dims
        for (int blacklistedDim : Configs.blackListedDims) {
            if (currentDim == blacklistedDim) {
                this.replacementCaves.func_151539_a(p_151539_1_, worldIn, x, z, blocks);
                return;
            }
        }

        displacementNoisePerlin.setSeed((int) worldIn.getSeed());

        debugValueAdjustments();
        // spotless:off
//        boolean logTime = false; // TODO turn off
//        long start = 0;
//        if (logTime) {
//            start = System.nanoTime();
//        }
        // spotless:on

        this.generateWorleyCaves(x, z, blocks);

        // spotless:off
//        if (logTime) {
//            genTime[currentTimeIndex] = System.nanoTime() - start;// System.currentTimeMillis() - start;
//            sum += genTime[currentTimeIndex];
//            currentTimeIndex++;
//            if (currentTimeIndex == genTime.length) {
//                System.out.printf(
//                    "%d chunk average: %.2f ms per chunk\n",
//                    numLogChunks,
//                    sum / ((float) numLogChunks * 1000000));
//                sum = 0;
//                currentTimeIndex = 0;
//            }
//        }
        // spotless:on
    }

    protected void generateWorleyCaves(int chunkX, int chunkZ, Block[] blocks) {
        int chunkMaxHeight = getMaxSurfaceHeight(blocks);
        int seaLevel = 63; // worldIn.getSeaLevel();
        Pair<FloatArray3D, BitArray2D> sampled = sampleNoise(chunkX, chunkZ, chunkMaxHeight + 1);

        FloatArray3D samples = sampled.getLeft();
        BitArray2D carves = sampled.getRight();

        float oneQuarter = 0.25F;
        float oneHalf = 0.5F;
        BiomeGenBase currentBiome;
        // float cutoffAdjuster = 0F; //TODO one day, perlin adjustments to cutoff

        // each chunk divided into 4 subchunks along X axis
        for (int x = 0; x < 4; x++) {
            // each chunk divided into 4 subchunks along Z axis
            for (int z = 0; z < 4; z++) {
                int depth = 0;

                // don't bother checking all the other logic if there's nothing to dig in this column
                if (!carves.get(x, z) && !carves.get(x + 1, z) && !carves.get(x, z + 1) && !carves.get(x + 1, z + 1))
                    continue;

                // each chunk divided into 128 subchunks along Y axis. Need lots of y sample points to not break things
                for (int y = (maxCaveHeight / 2) - 1; y >= 0; y--) {
                    // grab the 8 sample points needed from the noise values
                    float x0y0z0 = samples.get(x, y, z);
                    float x0y0z1 = samples.get(x, y, z + 1);
                    float x1y0z0 = samples.get(x + 1, y, z);
                    float x1y0z1 = samples.get(x + 1, y, z + 1);
                    float x0y1z0 = samples.get(x, y + 1, z);
                    float x0y1z1 = samples.get(x, y + 1, z + 1);
                    float x1y1z0 = samples.get(x + 1, y + 1, z);
                    float x1y1z1 = samples.get(x + 1, y + 1, z + 1);

                    // how much to increment noise along y value
                    // linear interpolation from start y and end y
                    float noiseStepY00 = (x0y1z0 - x0y0z0) * -oneHalf;
                    float noiseStepY01 = (x0y1z1 - x0y0z1) * -oneHalf;
                    float noiseStepY10 = (x1y1z0 - x1y0z0) * -oneHalf;
                    float noiseStepY11 = (x1y1z1 - x1y0z1) * -oneHalf;

                    // noise values of 4 corners at y=0
                    float noiseStartX0 = x0y0z0;
                    float noiseStartX1 = x0y0z1;
                    float noiseEndX0 = x1y0z0;
                    float noiseEndX1 = x1y0z1;

                    // loop through 2 blocks of the Y subchunk
                    for (int suby = 1; suby >= 0; suby--) {
                        int localY = suby + y * 2;
                        float noiseStartZ = noiseStartX0;
                        float noiseEndZ = noiseStartX1;

                        // how much to increment X values, linear interpolation
                        float noiseStepX0 = (noiseEndX0 - noiseStartX0) * oneQuarter;
                        float noiseStepX1 = (noiseEndX1 - noiseStartX1) * oneQuarter;

                        // loop through 4 blocks of the X subchunk
                        for (int subx = 0; subx < 4; subx++) {
                            int localX = subx + x * 4;
                            int realX = localX + chunkX * 16;

                            // how much to increment Z values, linear interpolation
                            float noiseStepZ = (noiseEndZ - noiseStartZ) * oneQuarter;

                            // Y and X already interpolated, just need to interpolate final 4 Z block to get final noise
                            // value
                            float noiseVal = noiseStartZ;

                            // loop through 4 blocks of the Z subchunk
                            for (int subz = 0; subz < 4; subz++) {
                                int localZ = subz + z * 4;
                                int realZ = localZ + chunkZ * 16;
                                currentBiome = null;

                                if (depth == 0) {
                                    // only checks depth once per 4x4 subchunk
                                    if (subx == 0 && subz == 0) {
                                        Block currentBlock = getBlock(blocks, localX, localY, localZ);
                                        currentBiome = worldObj.provider.worldChunkMgr.getBiomeGenAt(realX, realZ);

                                        // use isDigable to skip leaves/wood getting counted as surface
                                        if (canReplaceBlock(currentBlock, Blocks.air)
                                            || isBiomeBlock(currentBlock, currentBiome)) {
                                            depth++;
                                        }
                                    } else {
                                        continue;
                                    }
                                } else if (subx == 0 && subz == 0) {
                                    // already hit surface, simply increment depth counter
                                    depth++;
                                }

                                float adjustedNoiseCutoff = noiseCutoff;// + cutoffAdjuster;
                                if (depth < easeInDepth) {
                                    // higher threshold at surface, normal threshold below easeInDepth
                                    adjustedNoiseCutoff = (float) clampedLerp(
                                        noiseCutoff,
                                        surfaceCutoff,
                                        (easeInDepth - (float) depth) / easeInDepth);

                                }

                                // increase cutoff as we get closer to the minCaveHeight so it's not all flat floors
                                if (localY < (minCaveHeight + 5)) {
                                    adjustedNoiseCutoff += ((minCaveHeight + 5) - localY) * 0.05;
                                }

                                if (noiseVal > adjustedNoiseCutoff) {
                                    Block aboveBlock = getBlock(blocks, localX, localY + 1, localZ);
                                    if (aboveBlock == null) aboveBlock = Blocks.air;

                                    if (!isFluidBlock(aboveBlock) || localY <= lavaDepth) {
                                        // if we are in the easeInDepth range or near sea level or subH2O is installed,
                                        // do some extra checks for water before digging
                                        if ((depth < easeInDepth || localY > (seaLevel - 8) || additionalWaterChecks)
                                            && localY > lavaDepth) {
                                            if (localX < 15)
                                                if (isFluidBlock(getBlock(blocks, localX + 1, localY, localZ)))
                                                    continue;
                                            if (localX > 0)
                                                if (isFluidBlock(getBlock(blocks, localX - 1, localY, localZ)))
                                                    continue;
                                            if (localZ < 15)
                                                if (isFluidBlock(getBlock(blocks, localX, localY, localZ + 1)))
                                                    continue;
                                            if (localZ > 0)
                                                if (isFluidBlock(getBlock(blocks, localX, localY, localZ - 1)))
                                                    continue;
                                        }
                                        Block currentBlock = getBlock(blocks, localX, localY, localZ);
                                        if (currentBiome == null)
                                            currentBiome = worldObj.provider.worldChunkMgr.getBiomeGenAt(realX, realZ);// world.getBiome(realPos);

                                        boolean foundTopBlock = isTopBlock(currentBlock, currentBiome);
                                        digBlock(
                                            blocks,
                                            localX,
                                            localY,
                                            localZ,
                                            foundTopBlock,
                                            currentBlock,
                                            aboveBlock,
                                            currentBiome);
                                    }
                                }

                                noiseVal += noiseStepZ;
                            }

                            noiseStartZ += noiseStepX0;
                            noiseEndZ += noiseStepX1;
                        }

                        noiseStartX0 += noiseStepY00;
                        noiseStartX1 += noiseStepY01;
                        noiseEndX0 += noiseStepY10;
                        noiseEndX1 += noiseStepY11;
                    }
                }
            }
        }
    }

    public Pair<FloatArray3D, BitArray2D> sampleNoise(int chunkX, int chunkZ, int maxSurfaceHeight) {
        int originalMaxHeight = 128;

        FloatArray3D noiseSamples = new FloatArray3D(5, 128, 5);
        BitArray2D carves = new BitArray2D(5, 5);

        float noise;
        for (int x = 0; x < 5; x++) {
            int realX = x * 4 + chunkX * 16;
            for (int z = 0; z < 5; z++) {
                int realZ = z * 4 + chunkZ * 16;

                boolean columnHasCaveFlag = false;

                // loop from top down for y values so we can adjust noise above current y later on
                for (int y = 127; y >= 0; y--) {
                    float realY = y * 2;
                    if (realY > maxSurfaceHeight || realY > maxCaveHeight || realY < minCaveHeight) {
                        // if outside of valid cave range set noise value below normal minimum of -1.0
                        noiseSamples.set(x, y, z, -1.1F);
                    } else {
                        // Experiment making the cave system more chaotic the more you descend
                        /// TODO might be too dramatic down at lava level
                        float dispAmp = (float) (warpAmplifier
                            * ((originalMaxHeight - y) / (originalMaxHeight * 0.85)));

                        float xDisp = 0f;
                        float yDisp = 0f;
                        float zDisp = 0f;

                        xDisp = displacementNoisePerlin.getNoise(realX, realZ) * dispAmp;
                        yDisp = displacementNoisePerlin.getNoise(realX, realZ + 67.0f) * dispAmp;
                        zDisp = displacementNoisePerlin.getNoise(realX, realZ + 149.0f) * dispAmp;

                        // doubling the y frequency to get some more caves
                        noise = worleyF1divF3.singleCellular3Edge(
                            realX * xzCompression + xDisp,
                            realY * yCompression + yDisp,
                            realZ * xzCompression + zDisp);
                        noiseSamples.set(x, y, z, noise);

                        if (noise > noiseCutoff) {
                            columnHasCaveFlag = true;
                            // if noise is below cutoff, adjust values of neighbors
                            // helps prevent caves fracturing during interpolation

                            if (x > 0)
                                noiseSamples.set(x - 1, y, z, (noise * 0.2f) + (noiseSamples.get(x - 1, y, z) * 0.8f));
                            if (z > 0)
                                noiseSamples.set(x, y, z - 1, (noise * 0.2f) + (noiseSamples.get(x, y, z - 1) * 0.8f));

                            // more heavily adjust y above 'air block' noise values to give players more headroom
                            if (y < 128) {
                                float noiseAbove = noiseSamples.get(x, y + 1, z);
                                if (noise > noiseAbove)
                                    noiseSamples.set(x, y + 1, z, (noise * 0.8F) + (noiseAbove * 0.2F));
                                if (y < 127) {
                                    float noiseTwoAbove = noiseSamples.get(x, y + 2, z);
                                    if (noise > noiseTwoAbove)
                                        noiseSamples.set(x, y + 2, z, (noise * 0.35F) + (noiseTwoAbove * 0.65F));
                                }
                            }

                        }
                    }
                }
                carves.set(x, z, columnHasCaveFlag); // used to skip cave digging logic when we know
                // there is nothing to dig out
            }
        }
        return new ImmutablePair<>(noiseSamples, carves);
    }

    private int getSurfaceHeight(Block[] blocks, int localX, int localZ) {
        // Using a recursive binary search to find the surface
        return recursiveBinarySurfaceSearch(blocks, localX, localZ, 255, 0);
    }

    // Recursive binary search, this search always converges on the surface in 8 in cycles for the range 255 >= y >= 0
    private int recursiveBinarySurfaceSearch(Block[] blocks, int localX, int localZ, int searchTop, int searchBottom) {
        int top = searchTop;
        if (searchTop > searchBottom) {
            int searchMid = (searchBottom + searchTop) / 2;
            if (canReplaceBlock(getBlock(blocks, localX, searchMid, localZ), Blocks.air)) {
                top = recursiveBinarySurfaceSearch(blocks, localX, localZ, searchTop, searchMid + 1);
            } else {
                top = recursiveBinarySurfaceSearch(blocks, localX, localZ, searchMid, searchBottom);
            }
        }
        return top;
    }

    // tests 6 points in hexagon pattern get max height of chunk
    private int getMaxSurfaceHeight(Block[] blocks) {
        int max = 0;
        int[][] testcords = { { 2, 6 }, { 3, 11 }, { 7, 2 }, { 9, 13 }, { 12, 4 }, { 13, 9 } };

        for (int[] testcord : testcords) {
            int testmax = getSurfaceHeight(blocks, testcord[0], testcord[1]);
            if (testmax > max) {
                max = testmax;
                if (max > maxCaveHeight) return max;
            }
        }
        return max;
    }

    // returns true if block matches the top or filler block of the location biome
    private boolean isBiomeBlock(Block block, BiomeGenBase biome) {
        return block == biome.topBlock || block == biome.fillerBlock;
    }

    // returns true if block is fluid, trying to play nice with modded liquid
    private boolean isFluidBlock(Block state) {
        return state instanceof BlockLiquid || state instanceof IFluidBlock;
    }

    // Because it's private in MapGenCaves this is reimplemented
    // Determine if the block at the specified location is the top block for the biome, we take into account
    // Vanilla bugs to make sure that we generate the map the same way vanilla does.
    private boolean isTopBlock(Block block, BiomeGenBase biome) {
        // IBlockState block = data.getBlockState(x, y, z);
        return (isExceptionBiome(biome) ? block == Blocks.grass : block == biome.topBlock);
    }

    // Exception biomes to make sure we generate like vanilla
    private boolean isExceptionBiome(BiomeGenBase biome) {
        if (biome == BiomeGenBase.beach) return true;
        return biome == BiomeGenBase.desert;
    }

    protected boolean canReplaceBlock(Block block, Block above) {
        // Need to be able to replace not just vanilla stone + stuff
        return (Configs.allowReplaceMoreBlocks && block.getMaterial() == Material.rock) || (block == Blocks.stone
            || (block == Blocks.dirt || (block == Blocks.grass || (block == Blocks.hardened_clay
                || (block == Blocks.stained_hardened_clay || (block == Blocks.sandstone || (block == Blocks.mycelium
                    || (block == Blocks.snow_layer || (block == Blocks.sand || block == Blocks.gravel)
                        && above.getMaterial() != Material.water))))))));
    }

    /**
     * Digs out the current block, default implementation removes stone, filler, and top block
     * Sets the block to lava if y is less then 10, and air other wise.
     * If setting to air, it also checks to see if we've broken the surface and if so
     * tries to make the floor the biome's top block
     *
     * @param data     Block data array
     * @param x        local X position
     * @param y        local Y position
     * @param z        local Z position
     * @param foundTop True if we've encountered the biome's top block. Ideally if we've broken the surface.
     */
    protected void digBlock(Block[] data, int x, int y, int z, boolean foundTop, Block block, Block up,
        BiomeGenBase biome) {
        Block top = biome.topBlock;
        Block filler = biome.fillerBlock;

        if (this.canReplaceBlock(block, up) || block == top || block == filler) {
            if (y <= lavaDepth) {
                data[getBlockIndex(x, y, z)] = lava;
            } else {
                data[getBlockIndex(x, y, z)] = Blocks.air;

                if (foundTop && getBlock(data, x, y - 1, z) == filler) {
                    data[getBlockIndex(x, y - 1, z)] = top;
                }
            }
        }
    }

    private static int getBlockIndex(int x, int y, int z) {
        return x << 12 | z << 8 | y; // 12 = log2(chunkLength) + log2(chunkHeight), 8 = log2(chunkHeight)
    }

    private static Block getBlock(Block[] blocks, int x, int y, int z) {
        Block block = blocks[getBlockIndex(x, y, z)];

        return block == null ? Blocks.air : block;
    }

    public static double clampedLerp(double lowerBnd, double upperBnd, double slide) {
        if (slide < 0.0D) {
            return lowerBnd;
        } else {
            return slide > 1.0D ? upperBnd : lerp(slide, lowerBnd, upperBnd);
        }
    }

    public static double lerp(double pct, double start, double end) {
        return start + pct * (end - start);
    }
}
