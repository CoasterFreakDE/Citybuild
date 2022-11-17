package net.minecraft.world.level.dimension;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class LevelStem {
    public static final Codec<LevelStem> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(DimensionType.CODEC.fieldOf("type").forGetter(LevelStem::typeHolder), ChunkGenerator.CODEC.fieldOf("generator").forGetter(LevelStem::generator)).apply(instance, instance.stable(LevelStem::new));
    });
    public static final ResourceKey<LevelStem> OVERWORLD = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("overworld"));
    public static final ResourceKey<LevelStem> NETHER = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("the_nether"));
    public static final ResourceKey<LevelStem> END = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("the_end"));
    private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER = ImmutableSet.of(OVERWORLD, NETHER, END);
    private final Holder<DimensionType> type;
    private final ChunkGenerator generator;

    public LevelStem(Holder<DimensionType> dimensionTypeEntry, ChunkGenerator chunkGenerator) {
        this.type = dimensionTypeEntry;
        this.generator = chunkGenerator;
    }

    public Holder<DimensionType> typeHolder() {
        return this.type;
    }

    public ChunkGenerator generator() {
        return this.generator;
    }

    public static Stream<ResourceKey<LevelStem>> keysInOrder(Stream<ResourceKey<LevelStem>> stream) {
        return Stream.concat(BUILTIN_ORDER.stream(), stream.filter((registryKey) -> {
            return !BUILTIN_ORDER.contains(registryKey);
        }));
    }

    public static Registry<LevelStem> sortMap(Registry<LevelStem> registry) {
        WritableRegistry<LevelStem> writableRegistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
        keysInOrder(registry.registryKeySet().stream()).forEach((registryKey) -> {
            LevelStem levelStem = registry.get(registryKey);
            if (levelStem != null) {
                writableRegistry.register(registryKey, levelStem, registry.lifecycle(levelStem));
            }

        });
        return writableRegistry;
    }

    public static boolean stable(Registry<LevelStem> registry) {
        if (registry.size() != BUILTIN_ORDER.size()) {
            return false;
        } else {
            Optional<LevelStem> optional = registry.getOptional(OVERWORLD);
            Optional<LevelStem> optional2 = registry.getOptional(NETHER);
            Optional<LevelStem> optional3 = registry.getOptional(END);
            if (!optional.isEmpty() && !optional2.isEmpty() && !optional3.isEmpty()) {
                if (!optional.get().typeHolder().is(BuiltinDimensionTypes.OVERWORLD) && !optional.get().typeHolder().is(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
                    return false;
                } else if (!optional2.get().typeHolder().is(BuiltinDimensionTypes.NETHER)) {
                    return false;
                } else if (!optional3.get().typeHolder().is(BuiltinDimensionTypes.END)) {
                    return false;
                } else if (optional2.get().generator() instanceof NoiseBasedChunkGenerator && optional3.get().generator() instanceof NoiseBasedChunkGenerator) {
                    NoiseBasedChunkGenerator noiseBasedChunkGenerator = (NoiseBasedChunkGenerator)optional2.get().generator();
                    NoiseBasedChunkGenerator noiseBasedChunkGenerator2 = (NoiseBasedChunkGenerator)optional3.get().generator();
                    if (!noiseBasedChunkGenerator.stable(NoiseGeneratorSettings.NETHER)) {
                        return false;
                    } else if (!noiseBasedChunkGenerator2.stable(NoiseGeneratorSettings.END)) {
                        return false;
                    } else if (!(noiseBasedChunkGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource)) {
                        return false;
                    } else {
                        MultiNoiseBiomeSource multiNoiseBiomeSource = (MultiNoiseBiomeSource)noiseBasedChunkGenerator.getBiomeSource();
                        if (!multiNoiseBiomeSource.stable(MultiNoiseBiomeSource.Preset.NETHER)) {
                            return false;
                        } else {
                            BiomeSource biomeSource = optional.get().generator().getBiomeSource();
                            if (biomeSource instanceof MultiNoiseBiomeSource && !((MultiNoiseBiomeSource)biomeSource).stable(MultiNoiseBiomeSource.Preset.OVERWORLD)) {
                                return false;
                            } else {
                                return noiseBasedChunkGenerator2.getBiomeSource() instanceof TheEndBiomeSource;
                            }
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}
