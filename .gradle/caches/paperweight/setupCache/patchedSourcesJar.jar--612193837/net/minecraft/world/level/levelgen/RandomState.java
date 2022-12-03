package net.minecraft.world.level.levelgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class RandomState {
    final PositionalRandomFactory random;
    private final long legacyLevelSeed;
    private final Registry<NormalNoise.NoiseParameters> noises;
    private final NoiseRouter router;
    private final Climate.Sampler sampler;
    private final SurfaceSystem surfaceSystem;
    private final PositionalRandomFactory aquiferRandom;
    private final PositionalRandomFactory oreRandom;
    private final Map<ResourceKey<NormalNoise.NoiseParameters>, NormalNoise> noiseIntances;
    private final Map<ResourceLocation, PositionalRandomFactory> positionalRandoms;

    public static RandomState create(RegistryAccess dynamicRegistryManager, ResourceKey<NoiseGeneratorSettings> chunkGeneratorSettingsKey, long legacyWorldSeed) {
        return create(dynamicRegistryManager.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).getOrThrow(chunkGeneratorSettingsKey), dynamicRegistryManager.registryOrThrow(Registry.NOISE_REGISTRY), legacyWorldSeed);
    }

    public static RandomState create(NoiseGeneratorSettings chunkGeneratorSettings, Registry<NormalNoise.NoiseParameters> noiseParametersRegistry, long legacyWorldSeed) {
        return new RandomState(chunkGeneratorSettings, noiseParametersRegistry, legacyWorldSeed);
    }

    private RandomState(NoiseGeneratorSettings chunkGeneratorSettings, Registry<NormalNoise.NoiseParameters> noiseRegistry, final long seed) {
        this.random = chunkGeneratorSettings.getRandomSource().newInstance(seed).forkPositional();
        this.legacyLevelSeed = seed;
        this.noises = noiseRegistry;
        this.aquiferRandom = this.random.fromHashOf(new ResourceLocation("aquifer")).forkPositional();
        this.oreRandom = this.random.fromHashOf(new ResourceLocation("ore")).forkPositional();
        this.noiseIntances = new ConcurrentHashMap<>();
        this.positionalRandoms = new ConcurrentHashMap<>();
        this.surfaceSystem = new SurfaceSystem(this, chunkGeneratorSettings.defaultBlock(), chunkGeneratorSettings.seaLevel(), this.random);
        final boolean bl = chunkGeneratorSettings.useLegacyRandomSource();

        class NoiseWiringHelper implements DensityFunction.Visitor {
            private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

            private RandomSource newLegacyInstance(long seedx) {
                return new LegacyRandomSource(seed + seed);
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noiseDensityFunction) {
                Holder<NormalNoise.NoiseParameters> holder = noiseDensityFunction.noiseData();
                if (bl) {
                    if (Objects.equals(holder.unwrapKey(), Optional.of(Noises.TEMPERATURE))) {
                        NormalNoise normalNoise = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(0L), new NormalNoise.NoiseParameters(-7, 1.0D, 1.0D));
                        return new DensityFunction.NoiseHolder(holder, normalNoise);
                    }

                    if (Objects.equals(holder.unwrapKey(), Optional.of(Noises.VEGETATION))) {
                        NormalNoise normalNoise2 = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(1L), new NormalNoise.NoiseParameters(-7, 1.0D, 1.0D));
                        return new DensityFunction.NoiseHolder(holder, normalNoise2);
                    }

                    if (Objects.equals(holder.unwrapKey(), Optional.of(Noises.SHIFT))) {
                        NormalNoise normalNoise3 = NormalNoise.create(RandomState.this.random.fromHashOf(Noises.SHIFT.location()), new NormalNoise.NoiseParameters(0, 0.0D));
                        return new DensityFunction.NoiseHolder(holder, normalNoise3);
                    }
                }

                NormalNoise normalNoise4 = RandomState.this.getOrCreateNoise(holder.unwrapKey().orElseThrow());
                return new DensityFunction.NoiseHolder(holder, normalNoise4);
            }

            private DensityFunction wrapNew(DensityFunction densityFunction) {
                if (densityFunction instanceof BlendedNoise blendedNoise) {
                    RandomSource randomSource = bl ? this.newLegacyInstance(0L) : RandomState.this.random.fromHashOf(new ResourceLocation("terrain"));
                    return blendedNoise.withNewRandom(randomSource);
                } else {
                    return (DensityFunction)(densityFunction instanceof DensityFunctions.EndIslandDensityFunction ? new DensityFunctions.EndIslandDensityFunction(seed) : densityFunction);
                }
            }

            @Override
            public DensityFunction apply(DensityFunction densityFunction) {
                return this.wrapped.computeIfAbsent(densityFunction, this::wrapNew);
            }
        }

        this.router = chunkGeneratorSettings.noiseRouter().mapAll(new NoiseWiringHelper());
        this.sampler = new Climate.Sampler(this.router.temperature(), this.router.vegetation(), this.router.continents(), this.router.erosion(), this.router.depth(), this.router.ridges(), chunkGeneratorSettings.spawnTarget());
    }

    public NormalNoise getOrCreateNoise(ResourceKey<NormalNoise.NoiseParameters> noiseParametersKey) {
        return this.noiseIntances.computeIfAbsent(noiseParametersKey, (key) -> {
            return Noises.instantiate(this.noises, this.random, noiseParametersKey);
        });
    }

    public PositionalRandomFactory getOrCreateRandomFactory(ResourceLocation id) {
        return this.positionalRandoms.computeIfAbsent(id, (id2) -> {
            return this.random.fromHashOf(id).forkPositional();
        });
    }

    public long legacyLevelSeed() {
        return this.legacyLevelSeed;
    }

    public NoiseRouter router() {
        return this.router;
    }

    public Climate.Sampler sampler() {
        return this.sampler;
    }

    public SurfaceSystem surfaceSystem() {
        return this.surfaceSystem;
    }

    public PositionalRandomFactory aquiferRandom() {
        return this.aquiferRandom;
    }

    public PositionalRandomFactory oreRandom() {
        return this.oreRandom;
    }
}
