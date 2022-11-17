package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.google.common.base.Suppliers;
import com.mojang.datafixers.Products;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public abstract class ChunkGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<ChunkGenerator> CODEC = Registry.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
    public final Registry<StructureSet> structureSets;
    protected final BiomeSource biomeSource;
    private final Supplier<List<FeatureSorter.StepFeatureData>> featuresPerStep;
    public final Optional<HolderSet<StructureSet>> structureOverrides;
    private final Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter;
    private final Map<Structure, List<StructurePlacement>> placementsForStructure = new Object2ObjectOpenHashMap<>();
    private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions = new Object2ObjectArrayMap<>();
    private boolean hasGeneratedPositions;

    protected static <T extends ChunkGenerator> Products.P1<RecordCodecBuilder.Mu<T>, Registry<StructureSet>> commonCodec(RecordCodecBuilder.Instance<T> instance) {
        return instance.group(RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter((chunkGenerator) -> {
            return chunkGenerator.structureSets;
        }));
    }

    public ChunkGenerator(Registry<StructureSet> structureSetRegistry, Optional<HolderSet<StructureSet>> structureOverrides, BiomeSource biomeSource) {
        this(structureSetRegistry, structureOverrides, biomeSource, (biomeEntry) -> {
            return biomeEntry.value().getGenerationSettings();
        });
    }

    public ChunkGenerator(Registry<StructureSet> structureSetRegistry, Optional<HolderSet<StructureSet>> structureOverrides, BiomeSource biomeSource, Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter) {
        this.structureSets = structureSetRegistry;
        this.biomeSource = biomeSource;
        this.generationSettingsGetter = generationSettingsGetter;
        this.structureOverrides = structureOverrides;
        this.featuresPerStep = Suppliers.memoize(() -> {
            return FeatureSorter.buildFeaturesPerStep(List.copyOf(biomeSource.possibleBiomes()), (biomeEntry) -> {
                return generationSettingsGetter.apply(biomeEntry).features();
            }, true);
        });
    }

    public Stream<Holder<StructureSet>> possibleStructureSets() {
        return this.structureOverrides.isPresent() ? this.structureOverrides.get().stream() : this.structureSets.holders().map(Holder::hackyErase);
    }

    private void generatePositions(RandomState noiseConfig) {
        Set<Holder<Biome>> set = this.biomeSource.possibleBiomes();
        this.possibleStructureSets().forEach((structureSet) -> {
            StructureSet structureSet2 = structureSet.value();
            boolean bl = false;

            for(StructureSet.StructureSelectionEntry structureSelectionEntry : structureSet2.structures()) {
                Structure structure = structureSelectionEntry.structure().value();
                if (structure.biomes().stream().anyMatch(set::contains)) {
                    this.placementsForStructure.computeIfAbsent(structure, (structureType) -> {
                        return new ArrayList();
                    }).add(structureSet2.placement());
                    bl = true;
                }
            }

            if (bl) {
                StructurePlacement structurePlacement = structureSet2.placement();
                if (structurePlacement instanceof ConcentricRingsStructurePlacement) {
                    ConcentricRingsStructurePlacement concentricRingsStructurePlacement = (ConcentricRingsStructurePlacement)structurePlacement;
                    this.ringPositions.put(concentricRingsStructurePlacement, this.generateRingPositions(structureSet, noiseConfig, concentricRingsStructurePlacement));
                }
            }

        });
    }

    private CompletableFuture<List<ChunkPos>> generateRingPositions(Holder<StructureSet> structureSet, RandomState noiseConfig, ConcentricRingsStructurePlacement concentricRingsStructurePlacement) {
        return concentricRingsStructurePlacement.count() == 0 ? CompletableFuture.completedFuture(List.of()) : CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("placement calculation", () -> {
            Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
            List<ChunkPos> list = new ArrayList<>();
            int i = concentricRingsStructurePlacement.distance();
            int j = concentricRingsStructurePlacement.count();
            int k = concentricRingsStructurePlacement.spread();
            HolderSet<Biome> holderSet = concentricRingsStructurePlacement.preferredBiomes();
            RandomSource randomSource = RandomSource.create();
            randomSource.setSeed(this instanceof FlatLevelSource ? 0L : noiseConfig.legacyLevelSeed());
            double d = randomSource.nextDouble() * Math.PI * 2.0D;
            int l = 0;
            int m = 0;

            for(int n = 0; n < j; ++n) {
                double e = (double)(4 * i + i * m * 6) + (randomSource.nextDouble() - 0.5D) * (double)i * 2.5D;
                int o = (int)Math.round(Math.cos(d) * e);
                int p = (int)Math.round(Math.sin(d) * e);
                Pair<BlockPos, Holder<Biome>> pair = this.biomeSource.findBiomeHorizontal(SectionPos.sectionToBlockCoord(o, 8), 0, SectionPos.sectionToBlockCoord(p, 8), 112, holderSet::contains, randomSource, noiseConfig.sampler());
                if (pair != null) {
                    BlockPos blockPos = pair.getFirst();
                    o = SectionPos.blockToSectionCoord(blockPos.getX());
                    p = SectionPos.blockToSectionCoord(blockPos.getZ());
                }

                list.add(new ChunkPos(o, p));
                d += (Math.PI * 2D) / (double)k;
                ++l;
                if (l == k) {
                    ++m;
                    l = 0;
                    k += 2 * k / (m + 1);
                    k = Math.min(k, j - n);
                    d += randomSource.nextDouble() * Math.PI * 2.0D;
                }
            }

            double f = (double)stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0D;
            LOGGER.debug("Calculation for {} took {}s", structureSet, f);
            return list;
        }), Util.backgroundExecutor());
    }

    protected abstract Codec<? extends ChunkGenerator> codec();

    public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
        return Registry.CHUNK_GENERATOR.getResourceKey(this.codec());
    }

    public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> biomeRegistry, Executor executor, RandomState noiseConfig, Blender blender, StructureManager structureAccessor, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
            chunk.fillBiomesFromNoise(this.biomeSource, noiseConfig.sampler());
            return chunk;
        }), Util.backgroundExecutor());
    }

    public abstract void applyCarvers(WorldGenRegion chunkRegion, long seed, RandomState noiseConfig, BiomeManager biomeAccess, StructureManager structureAccessor, ChunkAccess chunk, GenerationStep.Carving carverStep);

    @Nullable
    public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel world, HolderSet<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures) {
        Map<StructurePlacement, Set<Holder<Structure>>> map = new Object2ObjectArrayMap<>();

        for(Holder<Structure> holder : structures) {
            for(StructurePlacement structurePlacement : this.getPlacementsForStructure(holder, world.getChunkSource().randomState())) {
                map.computeIfAbsent(structurePlacement, (placement) -> {
                    return new ObjectArraySet();
                }).add(holder);
            }
        }

        if (map.isEmpty()) {
            return null;
        } else {
            Pair<BlockPos, Holder<Structure>> pair = null;
            double d = Double.MAX_VALUE;
            StructureManager structureManager = world.structureManager();
            List<Map.Entry<StructurePlacement, Set<Holder<Structure>>>> list = new ArrayList<>(map.size());

            for(Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : map.entrySet()) {
                StructurePlacement structurePlacement2 = entry.getKey();
                if (structurePlacement2 instanceof ConcentricRingsStructurePlacement) {
                    ConcentricRingsStructurePlacement concentricRingsStructurePlacement = (ConcentricRingsStructurePlacement)structurePlacement2;
                    Pair<BlockPos, Holder<Structure>> pair2 = this.getNearestGeneratedStructure(entry.getValue(), world, structureManager, center, skipReferencedStructures, concentricRingsStructurePlacement);
                    if (pair2 != null) {
                        BlockPos blockPos = pair2.getFirst();
                        double e = center.distSqr(blockPos);
                        if (e < d) {
                            d = e;
                            pair = pair2;
                        }
                    }
                } else if (structurePlacement2 instanceof RandomSpreadStructurePlacement) {
                    list.add(entry);
                }
            }

            if (!list.isEmpty()) {
                int i = SectionPos.blockToSectionCoord(center.getX());
                int j = SectionPos.blockToSectionCoord(center.getZ());

                for(int k = 0; k <= radius; ++k) {
                    boolean bl = false;

                    for(Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry2 : list) {
                        RandomSpreadStructurePlacement randomSpreadStructurePlacement = (RandomSpreadStructurePlacement)entry2.getKey();
                        Pair<BlockPos, Holder<Structure>> pair3 = getNearestGeneratedStructure(entry2.getValue(), world, structureManager, i, j, k, skipReferencedStructures, world.getSeed(), randomSpreadStructurePlacement);
                        if (pair3 != null) {
                            bl = true;
                            double f = center.distSqr(pair3.getFirst());
                            if (f < d) {
                                d = f;
                                pair = pair3;
                            }
                        }
                    }

                    if (bl) {
                        return pair;
                    }
                }
            }

            return pair;
        }
    }

    @Nullable
    private Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(Set<Holder<Structure>> structures, ServerLevel world, StructureManager structureAccessor, BlockPos center, boolean skipReferencedStructures, ConcentricRingsStructurePlacement placement) {
        List<ChunkPos> list = this.getRingPositionsFor(placement, world.getChunkSource().randomState());
        if (list == null) {
            throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
        } else {
            Pair<BlockPos, Holder<Structure>> pair = null;
            double d = Double.MAX_VALUE;
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for(ChunkPos chunkPos : list) {
                mutableBlockPos.set(SectionPos.sectionToBlockCoord(chunkPos.x, 8), 32, SectionPos.sectionToBlockCoord(chunkPos.z, 8));
                double e = mutableBlockPos.distSqr(center);
                boolean bl = pair == null || e < d;
                if (bl) {
                    Pair<BlockPos, Holder<Structure>> pair2 = getStructureGeneratingAt(structures, world, structureAccessor, skipReferencedStructures, placement, chunkPos);
                    if (pair2 != null) {
                        pair = pair2;
                        d = e;
                    }
                }
            }

            return pair;
        }
    }

    @Nullable
    private static Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(Set<Holder<Structure>> structures, LevelReader world, StructureManager structureAccessor, int centerChunkX, int centerChunkZ, int radius, boolean skipReferencedStructures, long seed, RandomSpreadStructurePlacement placement) {
        int i = placement.spacing();

        for(int j = -radius; j <= radius; ++j) {
            boolean bl = j == -radius || j == radius;

            for(int k = -radius; k <= radius; ++k) {
                boolean bl2 = k == -radius || k == radius;
                if (bl || bl2) {
                    int l = centerChunkX + i * j;
                    int m = centerChunkZ + i * k;
                    ChunkPos chunkPos = placement.getPotentialStructureChunk(seed, l, m);
                    Pair<BlockPos, Holder<Structure>> pair = getStructureGeneratingAt(structures, world, structureAccessor, skipReferencedStructures, placement, chunkPos);
                    if (pair != null) {
                        return pair;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static Pair<BlockPos, Holder<Structure>> getStructureGeneratingAt(Set<Holder<Structure>> structures, LevelReader world, StructureManager structureAccessor, boolean skipReferencedStructures, StructurePlacement placement, ChunkPos pos) {
        for(Holder<Structure> holder : structures) {
            StructureCheckResult structureCheckResult = structureAccessor.checkStructurePresence(pos, holder.value(), skipReferencedStructures);
            if (structureCheckResult != StructureCheckResult.START_NOT_PRESENT) {
                if (!skipReferencedStructures && structureCheckResult == StructureCheckResult.START_PRESENT) {
                    return Pair.of(placement.getLocatePos(pos), holder);
                }

                ChunkAccess chunkAccess = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
                StructureStart structureStart = structureAccessor.getStartForStructure(SectionPos.bottomOf(chunkAccess), holder.value(), chunkAccess);
                if (structureStart != null && structureStart.isValid() && (!skipReferencedStructures || tryAddReference(structureAccessor, structureStart))) {
                    return Pair.of(placement.getLocatePos(structureStart.getChunkPos()), holder);
                }
            }
        }

        return null;
    }

    private static boolean tryAddReference(StructureManager structureAccessor, StructureStart start) {
        if (start.canBeReferenced()) {
            structureAccessor.addReference(start);
            return true;
        } else {
            return false;
        }
    }

    public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor) {
        ChunkPos chunkPos = chunk.getPos();
        if (!SharedConstants.debugVoidTerrain(chunkPos)) {
            SectionPos sectionPos = SectionPos.of(chunkPos, world.getMinSection());
            BlockPos blockPos = sectionPos.origin();
            Registry<Structure> registry = world.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
            Map<Integer, List<Structure>> map = registry.stream().collect(Collectors.groupingBy((structureType) -> {
                return structureType.step().ordinal();
            }));
            List<FeatureSorter.StepFeatureData> list = this.featuresPerStep.get();
            WorldgenRandom worldgenRandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
            long l = worldgenRandom.setDecorationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());
            Set<Holder<Biome>> set = new ObjectArraySet<>();
            ChunkPos.rangeClosed(sectionPos.chunk(), 1).forEach((chunkPosx) -> {
                ChunkAccess chunkAccess = world.getChunk(chunkPosx.x, chunkPosx.z);

                for(LevelChunkSection levelChunkSection : chunkAccess.getSections()) {
                    levelChunkSection.getBiomes().getAll(set::add);
                }

            });
            set.retainAll(this.biomeSource.possibleBiomes());
            int i = list.size();

            try {
                Registry<PlacedFeature> registry2 = world.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
                int j = Math.max(GenerationStep.Decoration.values().length, i);

                for(int k = 0; k < j; ++k) {
                    int m = 0;
                    if (structureAccessor.shouldGenerateStructures()) {
                        for(Structure structure : map.getOrDefault(k, Collections.emptyList())) {
                            worldgenRandom.setFeatureSeed(l, m, k);
                            Supplier<String> supplier = () -> {
                                return registry.getResourceKey(structure).map(Object::toString).orElseGet(structure::toString);
                            };

                            try {
                                world.setCurrentlyGenerating(supplier);
                                structureAccessor.startsForStructure(sectionPos, structure).forEach((start) -> {
                                    start.placeInChunk(world, structureAccessor, this, worldgenRandom, getWritableArea(chunk), chunkPos);
                                });
                            } catch (Exception var29) {
                                CrashReport crashReport = CrashReport.forThrowable(var29, "Feature placement");
                                crashReport.addCategory("Feature").setDetail("Description", supplier::get);
                                throw new ReportedException(crashReport);
                            }

                            ++m;
                        }
                    }

                    if (k < i) {
                        IntSet intSet = new IntArraySet();

                        for(Holder<Biome> holder : set) {
                            List<HolderSet<PlacedFeature>> list3 = this.generationSettingsGetter.apply(holder).features();
                            if (k < list3.size()) {
                                HolderSet<PlacedFeature> holderSet = list3.get(k);
                                FeatureSorter.StepFeatureData stepFeatureData = list.get(k);
                                holderSet.stream().map(Holder::value).forEach((placedFeaturex) -> {
                                    intSet.add(stepFeatureData.indexMapping().applyAsInt(placedFeaturex));
                                });
                            }
                        }

                        int n = intSet.size();
                        int[] is = intSet.toIntArray();
                        Arrays.sort(is);
                        FeatureSorter.StepFeatureData stepFeatureData2 = list.get(k);

                        for(int o = 0; o < n; ++o) {
                            int p = is[o];
                            PlacedFeature placedFeature = stepFeatureData2.features().get(p);
                            Supplier<String> supplier2 = () -> {
                                return registry2.getResourceKey(placedFeature).map(Object::toString).orElseGet(placedFeature::toString);
                            };
                            worldgenRandom.setFeatureSeed(l, p, k);

                            try {
                                world.setCurrentlyGenerating(supplier2);
                                placedFeature.placeWithBiomeCheck(world, this, worldgenRandom, blockPos);
                            } catch (Exception var30) {
                                CrashReport crashReport2 = CrashReport.forThrowable(var30, "Feature placement");
                                crashReport2.addCategory("Feature").setDetail("Description", supplier2::get);
                                throw new ReportedException(crashReport2);
                            }
                        }
                    }
                }

                world.setCurrentlyGenerating((Supplier<String>)null);
            } catch (Exception var31) {
                CrashReport crashReport3 = CrashReport.forThrowable(var31, "Biome decoration");
                crashReport3.addCategory("Generation").setDetail("CenterX", chunkPos.x).setDetail("CenterZ", chunkPos.z).setDetail("Seed", l);
                throw new ReportedException(crashReport3);
            }
        }
    }

    public boolean hasStructureChunkInRange(Holder<StructureSet> structureSet, RandomState noiseConfig, long seed, int chunkX, int chunkZ, int chunkRange) {
        StructureSet structureSet2 = structureSet.value();
        if (structureSet2 == null) {
            return false;
        } else {
            StructurePlacement structurePlacement = structureSet2.placement();

            for(int i = chunkX - chunkRange; i <= chunkX + chunkRange; ++i) {
                for(int j = chunkZ - chunkRange; j <= chunkZ + chunkRange; ++j) {
                    if (structurePlacement.isStructureChunk(this, noiseConfig, seed, i, j)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static BoundingBox getWritableArea(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        LevelHeightAccessor levelHeightAccessor = chunk.getHeightAccessorForGeneration();
        int k = levelHeightAccessor.getMinBuildHeight() + 1;
        int l = levelHeightAccessor.getMaxBuildHeight() - 1;
        return new BoundingBox(i, k, j, i + 15, l, j + 15);
    }

    public abstract void buildSurface(WorldGenRegion region, StructureManager structures, RandomState noiseConfig, ChunkAccess chunk);

    public abstract void spawnOriginalMobs(WorldGenRegion region);

    public int getSpawnHeight(LevelHeightAccessor world) {
        return 64;
    }

    public BiomeSource getBiomeSource() {
        return this.biomeSource;
    }

    public abstract int getGenDepth();

    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> biome, StructureManager accessor, MobCategory group, BlockPos pos) {
        Map<Structure, LongSet> map = accessor.getAllStructuresAt(pos);

        for(Map.Entry<Structure, LongSet> entry : map.entrySet()) {
            Structure structure = entry.getKey();
            StructureSpawnOverride structureSpawnOverride = structure.spawnOverrides().get(group);
            if (structureSpawnOverride != null) {
                MutableBoolean mutableBoolean = new MutableBoolean(false);
                Predicate<StructureStart> predicate = structureSpawnOverride.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE ? (start) -> {
                    return accessor.structureHasPieceAt(pos, start);
                } : (start) -> {
                    return start.getBoundingBox().isInside(pos);
                };
                accessor.fillStartsForStructure(structure, entry.getValue(), (start) -> {
                    if (mutableBoolean.isFalse() && predicate.test(start)) {
                        mutableBoolean.setTrue();
                    }

                });
                if (mutableBoolean.isTrue()) {
                    return structureSpawnOverride.spawns();
                }
            }
        }

        return biome.value().getMobSettings().getMobs(group);
    }

    public void createStructures(RegistryAccess registryManager, RandomState noiseConfig, StructureManager structureAccessor, ChunkAccess chunk, StructureTemplateManager structureTemplateManager, long seed) {
        ChunkPos chunkPos = chunk.getPos();
        SectionPos sectionPos = SectionPos.bottomOf(chunk);
        this.possibleStructureSets().forEach((structureSet) -> {
            StructurePlacement structurePlacement = structureSet.value().placement();
            List<StructureSet.StructureSelectionEntry> list = structureSet.value().structures();

            for(StructureSet.StructureSelectionEntry structureSelectionEntry : list) {
                StructureStart structureStart = structureAccessor.getStartForStructure(sectionPos, structureSelectionEntry.structure().value(), chunk);
                if (structureStart != null && structureStart.isValid()) {
                    return;
                }
            }

            if (structurePlacement.isStructureChunk(this, noiseConfig, seed, chunkPos.x, chunkPos.z)) {
                if (list.size() == 1) {
                    this.tryGenerateStructure(list.get(0), structureAccessor, registryManager, noiseConfig, structureTemplateManager, seed, chunk, chunkPos, sectionPos);
                } else {
                    ArrayList<StructureSet.StructureSelectionEntry> arrayList = new ArrayList<>(list.size());
                    arrayList.addAll(list);
                    WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
                    worldgenRandom.setLargeFeatureSeed(seed, chunkPos.x, chunkPos.z);
                    int i = 0;

                    for(StructureSet.StructureSelectionEntry structureSelectionEntry2 : arrayList) {
                        i += structureSelectionEntry2.weight();
                    }

                    while(!arrayList.isEmpty()) {
                        int j = worldgenRandom.nextInt(i);
                        int k = 0;

                        for(StructureSet.StructureSelectionEntry structureSelectionEntry3 : arrayList) {
                            j -= structureSelectionEntry3.weight();
                            if (j < 0) {
                                break;
                            }

                            ++k;
                        }

                        StructureSet.StructureSelectionEntry structureSelectionEntry4 = arrayList.get(k);
                        if (this.tryGenerateStructure(structureSelectionEntry4, structureAccessor, registryManager, noiseConfig, structureTemplateManager, seed, chunk, chunkPos, sectionPos)) {
                            return;
                        }

                        arrayList.remove(k);
                        i -= structureSelectionEntry4.weight();
                    }

                }
            }
        });
    }

    private boolean tryGenerateStructure(StructureSet.StructureSelectionEntry weightedEntry, StructureManager structureAccessor, RegistryAccess dynamicRegistryManager, RandomState noiseConfig, StructureTemplateManager structureManager, long seed, ChunkAccess chunk, ChunkPos pos, SectionPos sectionPos) {
        Structure structure = weightedEntry.structure().value();
        int i = fetchReferences(structureAccessor, chunk, sectionPos, structure);
        HolderSet<Biome> holderSet = structure.biomes();
        Predicate<Holder<Biome>> predicate = holderSet::contains;
        StructureStart structureStart = structure.generate(dynamicRegistryManager, this, this.biomeSource, noiseConfig, structureManager, seed, pos, i, chunk, predicate);
        if (structureStart.isValid()) {
            structureAccessor.setStartForStructure(sectionPos, structure, structureStart, chunk);
            return true;
        } else {
            return false;
        }
    }

    private static int fetchReferences(StructureManager structureAccessor, ChunkAccess chunk, SectionPos sectionPos, Structure structure) {
        StructureStart structureStart = structureAccessor.getStartForStructure(sectionPos, structure, chunk);
        return structureStart != null ? structureStart.getReferences() : 0;
    }

    public void createReferences(WorldGenLevel world, StructureManager structureAccessor, ChunkAccess chunk) {
        int i = 8;
        ChunkPos chunkPos = chunk.getPos();
        int j = chunkPos.x;
        int k = chunkPos.z;
        int l = chunkPos.getMinBlockX();
        int m = chunkPos.getMinBlockZ();
        SectionPos sectionPos = SectionPos.bottomOf(chunk);

        for(int n = j - 8; n <= j + 8; ++n) {
            for(int o = k - 8; o <= k + 8; ++o) {
                long p = ChunkPos.asLong(n, o);

                for(StructureStart structureStart : world.getChunk(n, o).getAllStarts().values()) {
                    try {
                        if (structureStart.isValid() && structureStart.getBoundingBox().intersects(l, m, l + 15, m + 15)) {
                            structureAccessor.addReferenceForStructure(sectionPos, structureStart.getStructure(), p, chunk);
                            DebugPackets.sendStructurePacket(world, structureStart);
                        }
                    } catch (Exception var21) {
                        CrashReport crashReport = CrashReport.forThrowable(var21, "Generating structure reference");
                        CrashReportCategory crashReportCategory = crashReport.addCategory("Structure");
                        Optional<? extends Registry<Structure>> optional = world.registryAccess().registry(Registry.STRUCTURE_REGISTRY);
                        crashReportCategory.setDetail("Id", () -> {
                            return optional.map((structureTypeRegistry) -> {
                                return structureTypeRegistry.getKey(structureStart.getStructure()).toString();
                            }).orElse("UNKNOWN");
                        });
                        crashReportCategory.setDetail("Name", () -> {
                            return Registry.STRUCTURE_TYPES.getKey(structureStart.getStructure().type()).toString();
                        });
                        crashReportCategory.setDetail("Class", () -> {
                            return structureStart.getStructure().getClass().getCanonicalName();
                        });
                        throw new ReportedException(crashReport);
                    }
                }
            }
        }

    }

    public abstract CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState noiseConfig, StructureManager structureAccessor, ChunkAccess chunk);

    public abstract int getSeaLevel();

    public abstract int getMinY();

    public abstract int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig);

    public abstract NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig);

    public int getFirstFreeHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        return this.getBaseHeight(x, z, heightmap, world, noiseConfig);
    }

    public int getFirstOccupiedHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig) {
        return this.getBaseHeight(x, z, heightmap, world, noiseConfig) - 1;
    }

    public void ensureStructuresGenerated(RandomState noiseConfig) {
        if (!this.hasGeneratedPositions) {
            this.generatePositions(noiseConfig);
            this.hasGeneratedPositions = true;
        }

    }

    @Nullable
    public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement structurePlacement, RandomState noiseConfig) {
        this.ensureStructuresGenerated(noiseConfig);
        CompletableFuture<List<ChunkPos>> completableFuture = this.ringPositions.get(structurePlacement);
        return completableFuture != null ? completableFuture.join() : null;
    }

    private List<StructurePlacement> getPlacementsForStructure(Holder<Structure> structureEntry, RandomState noiseConfig) {
        this.ensureStructuresGenerated(noiseConfig);
        return this.placementsForStructure.getOrDefault(structureEntry.value(), List.of());
    }

    public abstract void addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos);

    /** @deprecated */
    @Deprecated
    public BiomeGenerationSettings getBiomeGenerationSettings(Holder<Biome> biomeEntry) {
        return this.generationSettingsGetter.apply(biomeEntry);
    }
}
