package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.commons.lang3.StringUtils;

public class WorldGenSettings {
    public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.LONG.fieldOf("seed").stable().forGetter(WorldGenSettings::seed), Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldGenSettings::generateStructures), Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldGenSettings::generateBonusChest), RegistryCodecs.dataPackAwareCodec(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable(), LevelStem.CODEC).xmap(LevelStem::sortMap, Function.identity()).fieldOf("dimensions").forGetter(WorldGenSettings::dimensions), Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter((worldGenSettings) -> {
            return worldGenSettings.legacyCustomOptions;
        })).apply(instance, instance.stable(WorldGenSettings::new));
    }).comapFlatMap(WorldGenSettings::guardExperimental, Function.identity());
    private final long seed;
    private final boolean generateStructures;
    private final boolean generateBonusChest;
    private final Registry<LevelStem> dimensions;
    private final Optional<String> legacyCustomOptions;

    private DataResult<WorldGenSettings> guardExperimental() {
        LevelStem levelStem = this.dimensions.get(LevelStem.OVERWORLD);
        if (levelStem == null) {
            return DataResult.error("Overworld settings missing");
        } else {
            return this.stable() ? DataResult.success(this, Lifecycle.stable()) : DataResult.success(this);
        }
    }

    private boolean stable() {
        return LevelStem.stable(this.dimensions);
    }

    public WorldGenSettings(long seed, boolean generateStructures, boolean bonusChest, Registry<LevelStem> options) {
        this(seed, generateStructures, bonusChest, options, Optional.empty());
        LevelStem levelStem = options.get(LevelStem.OVERWORLD);
        if (levelStem == null) {
            throw new IllegalStateException("Overworld settings missing");
        }
    }

    private WorldGenSettings(long seed, boolean generateStructures, boolean bonusChest, Registry<LevelStem> options, Optional<String> legacyCustomOptions) {
        this.seed = seed;
        this.generateStructures = generateStructures;
        this.generateBonusChest = bonusChest;
        this.dimensions = options;
        this.legacyCustomOptions = legacyCustomOptions;
    }

    public long seed() {
        return this.seed;
    }

    public boolean generateStructures() {
        return this.generateStructures;
    }

    public boolean generateBonusChest() {
        return this.generateBonusChest;
    }

    public static WorldGenSettings replaceOverworldGenerator(RegistryAccess dynamicRegistryManager, WorldGenSettings generatorOptions, ChunkGenerator chunkGenerator) {
        Registry<DimensionType> registry = dynamicRegistryManager.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
        Registry<LevelStem> registry2 = withOverworld(registry, generatorOptions.dimensions(), chunkGenerator);
        return new WorldGenSettings(generatorOptions.seed(), generatorOptions.generateStructures(), generatorOptions.generateBonusChest(), registry2);
    }

    public static Registry<LevelStem> withOverworld(Registry<DimensionType> dimensionTypeRegistry, Registry<LevelStem> options, ChunkGenerator overworldGenerator) {
        LevelStem levelStem = options.get(LevelStem.OVERWORLD);
        Holder<DimensionType> holder = levelStem == null ? dimensionTypeRegistry.getOrCreateHolderOrThrow(BuiltinDimensionTypes.OVERWORLD) : levelStem.typeHolder();
        return withOverworld(options, holder, overworldGenerator);
    }

    public static Registry<LevelStem> withOverworld(Registry<LevelStem> options, Holder<DimensionType> dimensionType, ChunkGenerator overworldGenerator) {
        WritableRegistry<LevelStem> writableRegistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
        writableRegistry.register(LevelStem.OVERWORLD, new LevelStem(dimensionType, overworldGenerator), Lifecycle.stable());

        for(Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : options.entrySet()) {
            ResourceKey<LevelStem> resourceKey = entry.getKey();
            if (resourceKey != LevelStem.OVERWORLD) {
                writableRegistry.register(resourceKey, entry.getValue(), options.lifecycle(entry.getValue()));
            }
        }

        return writableRegistry;
    }

    public Registry<LevelStem> dimensions() {
        return this.dimensions;
    }

    public ChunkGenerator overworld() {
        LevelStem levelStem = this.dimensions.get(LevelStem.OVERWORLD);
        if (levelStem == null) {
            throw new IllegalStateException("Overworld settings missing");
        } else {
            return levelStem.generator();
        }
    }

    public ImmutableSet<ResourceKey<Level>> levels() {
        return this.dimensions().entrySet().stream().map(Map.Entry::getKey).map(WorldGenSettings::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
    }

    public static ResourceKey<Level> levelStemToLevel(ResourceKey<LevelStem> dimensionOptionsKey) {
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionOptionsKey.location());
    }

    public static ResourceKey<LevelStem> levelToLevelStem(ResourceKey<Level> worldKey) {
        return ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, worldKey.location());
    }

    public boolean isDebug() {
        return this.overworld() instanceof DebugLevelSource;
    }

    public boolean isFlatWorld() {
        return this.overworld() instanceof FlatLevelSource;
    }

    public boolean isOldCustomizedWorld() {
        return this.legacyCustomOptions.isPresent();
    }

    public WorldGenSettings withBonusChest() {
        return new WorldGenSettings(this.seed, this.generateStructures, true, this.dimensions, this.legacyCustomOptions);
    }

    public WorldGenSettings withStructuresToggled() {
        return new WorldGenSettings(this.seed, !this.generateStructures, this.generateBonusChest, this.dimensions);
    }

    public WorldGenSettings withBonusChestToggled() {
        return new WorldGenSettings(this.seed, this.generateStructures, !this.generateBonusChest, this.dimensions);
    }

    public WorldGenSettings withSeed(boolean hardcore, OptionalLong seed) {
        long l = seed.orElse(this.seed);
        Registry<LevelStem> registry;
        if (seed.isPresent()) {
            WritableRegistry<LevelStem> writableRegistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);

            for(Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : this.dimensions.entrySet()) {
                ResourceKey<LevelStem> resourceKey = entry.getKey();
                writableRegistry.register(resourceKey, new LevelStem(entry.getValue().typeHolder(), entry.getValue().generator()), this.dimensions.lifecycle(entry.getValue()));
            }

            registry = writableRegistry;
        } else {
            registry = this.dimensions;
        }

        WorldGenSettings worldGenSettings;
        if (this.isDebug()) {
            worldGenSettings = new WorldGenSettings(l, false, false, registry);
        } else {
            worldGenSettings = new WorldGenSettings(l, this.generateStructures(), this.generateBonusChest() && !hardcore, registry);
        }

        return worldGenSettings;
    }

    public static OptionalLong parseSeed(String seed) {
        seed = seed.trim();
        if (StringUtils.isEmpty(seed)) {
            return OptionalLong.empty();
        } else {
            try {
                return OptionalLong.of(Long.parseLong(seed));
            } catch (NumberFormatException var2) {
                return OptionalLong.of((long)seed.hashCode());
            }
        }
    }
}
