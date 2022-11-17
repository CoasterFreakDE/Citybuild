package net.minecraft.world.level.levelgen.presets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;

public class WorldPreset {
    public static final Codec<WorldPreset> DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.unboundedMap(ResourceKey.codec(Registry.LEVEL_STEM_REGISTRY), LevelStem.CODEC).fieldOf("dimensions").forGetter((preset) -> {
            return preset.dimensions;
        })).apply(instance, WorldPreset::new);
    }).flatXmap(WorldPreset::requireOverworld, WorldPreset::requireOverworld);
    public static final Codec<Holder<WorldPreset>> CODEC = RegistryFileCodec.create(Registry.WORLD_PRESET_REGISTRY, DIRECT_CODEC);
    private final Map<ResourceKey<LevelStem>, LevelStem> dimensions;

    public WorldPreset(Map<ResourceKey<LevelStem>, LevelStem> dimensions) {
        this.dimensions = dimensions;
    }

    private Registry<LevelStem> createRegistry() {
        WritableRegistry<LevelStem> writableRegistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
        LevelStem.keysInOrder(this.dimensions.keySet().stream()).forEach((registryKey) -> {
            LevelStem levelStem = this.dimensions.get(registryKey);
            if (levelStem != null) {
                writableRegistry.register(registryKey, levelStem, Lifecycle.stable());
            }

        });
        return writableRegistry.freeze();
    }

    public WorldGenSettings createWorldGenSettings(long seed, boolean generateStructures, boolean bonusChest) {
        return new WorldGenSettings(seed, generateStructures, bonusChest, this.createRegistry());
    }

    public WorldGenSettings recreateWorldGenSettings(WorldGenSettings generatorOptions) {
        return this.createWorldGenSettings(generatorOptions.seed(), generatorOptions.generateStructures(), generatorOptions.generateBonusChest());
    }

    public Optional<LevelStem> overworld() {
        return Optional.ofNullable(this.dimensions.get(LevelStem.OVERWORLD));
    }

    public LevelStem overworldOrThrow() {
        return this.overworld().orElseThrow(() -> {
            return new IllegalStateException("Can't find overworld in this preset");
        });
    }

    private static DataResult<WorldPreset> requireOverworld(WorldPreset preset) {
        return preset.overworld().isEmpty() ? DataResult.error("Missing overworld dimension") : DataResult.success(preset, Lifecycle.stable());
    }
}
