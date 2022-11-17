package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.apache.commons.lang3.mutable.MutableObject;

public final class NoiseBasedChunkGenerator extends ChunkGenerator {
    public static final Codec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> {
        return commonCodec(instance).and(instance.group(RegistryOps.retrieveRegistry(Registry.NOISE_REGISTRY).forGetter((generator) -> {
            return generator.noises;
        }), BiomeSource.CODEC.fieldOf("biome_source").forGetter((generator) -> {
            return generator.biomeSource;
        }), NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter((generator) -> {
            return generator.settings;
        }))).apply(instance, instance.stable(NoiseBasedChunkGenerator::new));
    });
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    protected final BlockState defaultBlock;
    public final Registry<NormalNoise.NoiseParameters> noises;
    public final Holder<NoiseGeneratorSettings> settings;
    private final Aquifer.FluidPicker globalFluidPicker;

    public NoiseBasedChunkGenerator(Registry<StructureSet> structureSetRegistry, Registry<NormalNoise.NoiseParameters> noiseRegistry, BiomeSource populationSource, Holder<NoiseGeneratorSettings> settings) {
        super(structureSetRegistry, Optional.empty(), populationSource);
        this.noises = noiseRegistry;
        this.settings = settings;
        NoiseGeneratorSettings noiseGeneratorSettings = this.settings.value();
        this.defaultBlock = noiseGeneratorSettings.defaultBlock();
        Aquifer.FluidStatus fluidStatus = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
        int i = noiseGeneratorSettings.seaLevel();
        Aquifer.FluidStatus fluidStatus2 = new Aquifer.FluidStatus(i, noiseGeneratorSettings.defaultFluid());
        Aquifer.FluidStatus fluidStatus3 = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
        this.globalFluidPicker = (x, y, z) -> {
            return y < Math.min(-54, i) ? fluidStatus : fluidStatus2;
        };
    }

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> biomeRegistry, Executor executor, RandomState noiseConfig, Blender blender, StructureManager structureAccessor, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
            this.doCreateBiomes(blender, noiseConfig, structureAccessor, chunk);
            return chunk;
        }), Util.backgroundExecutor());
    }

    private void doCreateBiomes(Blender blender, RandomState noiseConfig, StructureManager structureAccessor, ChunkAccess chunk) {
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk((chunkx) -> {
            return this.createNoiseChunk(chunkx, structureAccessor, blender, noiseConfig);
        });
        BiomeResolver biomeResolver = BelowZeroRetrogen.getBiomeResolver(blender.getBiomeResolver(this.biomeSource), chunk);
        chunk.fillBiomesFromNoise(biomeResolver, noiseChunk.cachedClimateSampler(noiseConfig.router(), this.settings.value().spawnTarget()));
    }

    private NoiseChunk createNoiseChunk(ChunkAccess chunk, StructureManager world, Blender blender, RandomState noiseConfig) {
        return NoiseChunk.forChunk(chunk, noiseConfig, Beardifier.forStructuresInChunk(world, chunk.getPos()), this.settings.value(), this.globalFluidPicker, blender);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public Holder<NoiseGeneratorSettings> generatorSettings() {
        return this.settings;
    }

    public boolean stable(ResourceKey<NoiseGeneratorSettings> settings) {
        return this.settings.is(settings);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        return this.iterateNoiseColumn(world, noiseConfig, x, z, (MutableObject<NoiseColumn>)null, heightmap.isOpaque()).orElse(world.getMinBuildHeight());
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig) {
        MutableObject<NoiseColumn> mutableObject = new MutableObject<>();
        this.iterateNoiseColumn(world, noiseConfig, x, z, mutableObject, (Predicate<BlockState>)null);
        return mutableObject.getValue();
    }

    @Override
    public void addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos) {
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        NoiseRouter noiseRouter = noiseConfig.router();
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ());
        double d = noiseRouter.ridges().compute(singlePointContext);
        text.add("NoiseRouter T: " + decimalFormat.format(noiseRouter.temperature().compute(singlePointContext)) + " V: " + decimalFormat.format(noiseRouter.vegetation().compute(singlePointContext)) + " C: " + decimalFormat.format(noiseRouter.continents().compute(singlePointContext)) + " E: " + decimalFormat.format(noiseRouter.erosion().compute(singlePointContext)) + " D: " + decimalFormat.format(noiseRouter.depth().compute(singlePointContext)) + " W: " + decimalFormat.format(d) + " PV: " + decimalFormat.format((double)NoiseRouterData.peaksAndValleys((float)d)) + " AS: " + decimalFormat.format(noiseRouter.initialDensityWithoutJaggedness().compute(singlePointContext)) + " N: " + decimalFormat.format(noiseRouter.finalDensity().compute(singlePointContext)));
    }

    private OptionalInt iterateNoiseColumn(LevelHeightAccessor world, RandomState noiseConfig, int x, int z, @Nullable MutableObject<NoiseColumn> columnSample, @Nullable Predicate<BlockState> stopPredicate) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(world);
        int i = noiseSettings.getCellHeight();
        int j = noiseSettings.minY();
        int k = Mth.intFloorDiv(j, i);
        int l = Mth.intFloorDiv(noiseSettings.height(), i);
        if (l <= 0) {
            return OptionalInt.empty();
        } else {
            BlockState[] blockStates;
            if (columnSample == null) {
                blockStates = null;
            } else {
                blockStates = new BlockState[noiseSettings.height()];
                columnSample.setValue(new NoiseColumn(j, blockStates));
            }

            int m = noiseSettings.getCellWidth();
            int n = Math.floorDiv(x, m);
            int o = Math.floorDiv(z, m);
            int p = Math.floorMod(x, m);
            int q = Math.floorMod(z, m);
            int r = n * m;
            int s = o * m;
            double d = (double)p / (double)m;
            double e = (double)q / (double)m;
            NoiseChunk noiseChunk = new NoiseChunk(1, noiseConfig, r, s, noiseSettings, DensityFunctions.BeardifierMarker.INSTANCE, this.settings.value(), this.globalFluidPicker, Blender.empty());
            noiseChunk.initializeForFirstCellX();
            noiseChunk.advanceCellX(0);

            for(int t = l - 1; t >= 0; --t) {
                noiseChunk.selectCellYZ(t, 0);

                for(int u = i - 1; u >= 0; --u) {
                    int v = (k + t) * i + u;
                    double f = (double)u / (double)i;
                    noiseChunk.updateForY(v, f);
                    noiseChunk.updateForX(x, d);
                    noiseChunk.updateForZ(z, e);
                    BlockState blockState = noiseChunk.getInterpolatedState();
                    BlockState blockState2 = blockState == null ? this.defaultBlock : blockState;
                    if (blockStates != null) {
                        int w = t * i + u;
                        blockStates[w] = blockState2;
                    }

                    if (stopPredicate != null && stopPredicate.test(blockState2)) {
                        noiseChunk.stopInterpolation();
                        return OptionalInt.of(v + 1);
                    }
                }
            }

            noiseChunk.stopInterpolation();
            return OptionalInt.empty();
        }
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState noiseConfig, ChunkAccess chunk) {
        if (!SharedConstants.debugVoidTerrain(chunk.getPos())) {
            WorldGenerationContext worldGenerationContext = new WorldGenerationContext(this, region);
            this.buildSurface(chunk, worldGenerationContext, noiseConfig, structures, region.getBiomeManager(), region.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), Blender.of(region));
        }
    }

    @VisibleForTesting
    public void buildSurface(ChunkAccess chunk, WorldGenerationContext heightContext, RandomState noiseConfig, StructureManager structureAccessor, BiomeManager biomeAccess, Registry<Biome> biomeRegistry, Blender blender) {
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk((chunkx) -> {
            return this.createNoiseChunk(chunkx, structureAccessor, blender, noiseConfig);
        });
        NoiseGeneratorSettings noiseGeneratorSettings = this.settings.value();
        noiseConfig.surfaceSystem().buildSurface(noiseConfig, biomeAccess, biomeRegistry, noiseGeneratorSettings.useLegacyRandomSource(), heightContext, chunk, noiseChunk, noiseGeneratorSettings.surfaceRule());
    }

    @Override
    public void applyCarvers(WorldGenRegion chunkRegion, long seed, RandomState noiseConfig, BiomeManager biomeAccess, StructureManager structureAccessor, ChunkAccess chunk, GenerationStep.Carving carverStep) {
        BiomeManager biomeManager = biomeAccess.withDifferentSource((biomeX, biomeY, biomeZ) -> {
            return this.biomeSource.getNoiseBiome(biomeX, biomeY, biomeZ, noiseConfig.sampler());
        });
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        int i = 8;
        ChunkPos chunkPos = chunk.getPos();
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk((chunkx) -> {
            return this.createNoiseChunk(chunkx, structureAccessor, Blender.of(chunkRegion), noiseConfig);
        });
        Aquifer aquifer = noiseChunk.aquifer();
        CarvingContext carvingContext = new CarvingContext(this, chunkRegion.registryAccess(), chunk.getHeightAccessorForGeneration(), noiseChunk, noiseConfig, this.settings.value().surfaceRule());
        CarvingMask carvingMask = ((ProtoChunk)chunk).getOrCreateCarvingMask(carverStep);

        for(int j = -8; j <= 8; ++j) {
            for(int k = -8; k <= 8; ++k) {
                ChunkPos chunkPos2 = new ChunkPos(chunkPos.x + j, chunkPos.z + k);
                ChunkAccess chunkAccess = chunkRegion.getChunk(chunkPos2.x, chunkPos2.z);
                BiomeGenerationSettings biomeGenerationSettings = chunkAccess.carverBiome(() -> {
                    return this.getBiomeGenerationSettings(this.biomeSource.getNoiseBiome(QuartPos.fromBlock(chunkPos2.getMinBlockX()), 0, QuartPos.fromBlock(chunkPos2.getMinBlockZ()), noiseConfig.sampler()));
                });
                Iterable<Holder<ConfiguredWorldCarver<?>>> iterable = biomeGenerationSettings.getCarvers(carverStep);
                int l = 0;

                for(Holder<ConfiguredWorldCarver<?>> holder : iterable) {
                    ConfiguredWorldCarver<?> configuredWorldCarver = holder.value();
                    worldgenRandom.setLargeFeatureSeed(seed + (long)l, chunkPos2.x, chunkPos2.z);
                    if (configuredWorldCarver.isStartChunk(worldgenRandom)) {
                        configuredWorldCarver.carve(carvingContext, chunk, biomeManager::getBiome, worldgenRandom, aquifer, chunkPos2, carvingMask);
                    }

                    ++l;
                }
            }
        }

    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState noiseConfig, StructureManager structureAccessor, ChunkAccess chunk) {
        NoiseSettings noiseSettings = this.settings.value().noiseSettings().clampToHeightAccessor(chunk.getHeightAccessorForGeneration());
        int i = noiseSettings.minY();
        int j = Mth.intFloorDiv(i, noiseSettings.getCellHeight());
        int k = Mth.intFloorDiv(noiseSettings.height(), noiseSettings.getCellHeight());
        if (k <= 0) {
            return CompletableFuture.completedFuture(chunk);
        } else {
            int l = chunk.getSectionIndex(k * noiseSettings.getCellHeight() - 1 + i);
            int m = chunk.getSectionIndex(i);
            Set<LevelChunkSection> set = Sets.newHashSet();

            for(int n = l; n >= m; --n) {
                LevelChunkSection levelChunkSection = chunk.getSection(n);
                levelChunkSection.acquire();
                set.add(levelChunkSection);
            }

            return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_fill_noise", () -> {
                return this.doFill(blender, structureAccessor, noiseConfig, chunk, j, k);
            }), Util.backgroundExecutor()).whenCompleteAsync((chunkAccess, throwable) -> {
                for(LevelChunkSection levelChunkSection : set) {
                    levelChunkSection.release();
                }

            }, executor);
        }
    }

    private ChunkAccess doFill(Blender blender, StructureManager structureAccessor, RandomState noiseConfig, ChunkAccess chunk, int minimumCellY, int cellHeight) {
        NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk((chunkx) -> {
            return this.createNoiseChunk(chunkx, structureAccessor, blender, noiseConfig);
        });
        Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        int k = noiseChunk.cellWidth();
        int l = noiseChunk.cellHeight();
        int m = 16 / k;
        int n = 16 / k;

        for(int o = 0; o < m; ++o) {
            noiseChunk.advanceCellX(o);

            for(int p = 0; p < n; ++p) {
                LevelChunkSection levelChunkSection = chunk.getSection(chunk.getSectionsCount() - 1);

                for(int q = cellHeight - 1; q >= 0; --q) {
                    noiseChunk.selectCellYZ(q, p);

                    for(int r = l - 1; r >= 0; --r) {
                        int s = (minimumCellY + q) * l + r;
                        int t = s & 15;
                        int u = chunk.getSectionIndex(s);
                        if (chunk.getSectionIndex(levelChunkSection.bottomBlockY()) != u) {
                            levelChunkSection = chunk.getSection(u);
                        }

                        double d = (double)r / (double)l;
                        noiseChunk.updateForY(s, d);

                        for(int v = 0; v < k; ++v) {
                            int w = i + o * k + v;
                            int x = w & 15;
                            double e = (double)v / (double)k;
                            noiseChunk.updateForX(w, e);

                            for(int y = 0; y < k; ++y) {
                                int z = j + p * k + y;
                                int aa = z & 15;
                                double f = (double)y / (double)k;
                                noiseChunk.updateForZ(z, f);
                                BlockState blockState = noiseChunk.getInterpolatedState();
                                if (blockState == null) {
                                    blockState = this.defaultBlock;
                                }

                                blockState = this.debugPreliminarySurfaceLevel(noiseChunk, w, s, z, blockState);
                                if (blockState != AIR && !SharedConstants.debugVoidTerrain(chunk.getPos())) {
                                    if (blockState.getLightEmission() != 0 && chunk instanceof ProtoChunk) {
                                        mutableBlockPos.set(w, s, z);
                                        ((ProtoChunk)chunk).addLight(mutableBlockPos);
                                    }

                                    levelChunkSection.setBlockState(x, t, aa, blockState, false);
                                    heightmap.update(x, s, aa, blockState);
                                    heightmap2.update(x, s, aa, blockState);
                                    if (aquifer.shouldScheduleFluidUpdate() && !blockState.getFluidState().isEmpty()) {
                                        mutableBlockPos.set(w, s, z);
                                        chunk.markPosForPostprocessing(mutableBlockPos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            noiseChunk.swapSlices();
        }

        noiseChunk.stopInterpolation();
        return chunk;
    }

    private BlockState debugPreliminarySurfaceLevel(NoiseChunk chunkNoiseSampler, int x, int y, int z, BlockState state) {
        return state;
    }

    @Override
    public int getGenDepth() {
        return this.settings.value().noiseSettings().height();
    }

    @Override
    public int getSeaLevel() {
        return this.settings.value().seaLevel();
    }

    @Override
    public int getMinY() {
        return this.settings.value().noiseSettings().minY();
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        if (!this.settings.value().disableMobGeneration()) {
            ChunkPos chunkPos = region.getCenter();
            Holder<Biome> holder = region.getBiome(chunkPos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
            WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
            worldgenRandom.setDecorationSeed(region.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
            NaturalSpawner.spawnMobsForChunkGeneration(region, holder, chunkPos, worldgenRandom);
        }
    }
}
