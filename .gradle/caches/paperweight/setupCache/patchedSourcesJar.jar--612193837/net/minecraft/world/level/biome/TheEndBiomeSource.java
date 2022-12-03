package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.DensityFunction;

public class TheEndBiomeSource extends BiomeSource {
    public static final Codec<TheEndBiomeSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter((biomeSource) -> {
            return null;
        })).apply(instance, instance.stable(TheEndBiomeSource::new));
    });
    private final Holder<Biome> end;
    private final Holder<Biome> highlands;
    private final Holder<Biome> midlands;
    private final Holder<Biome> islands;
    private final Holder<Biome> barrens;

    public TheEndBiomeSource(Registry<Biome> biomeRegistry) {
        this(biomeRegistry.getOrCreateHolderOrThrow(Biomes.THE_END), biomeRegistry.getOrCreateHolderOrThrow(Biomes.END_HIGHLANDS), biomeRegistry.getOrCreateHolderOrThrow(Biomes.END_MIDLANDS), biomeRegistry.getOrCreateHolderOrThrow(Biomes.SMALL_END_ISLANDS), biomeRegistry.getOrCreateHolderOrThrow(Biomes.END_BARRENS));
    }

    private TheEndBiomeSource(Holder<Biome> centerBiome, Holder<Biome> highlandsBiome, Holder<Biome> midlandsBiome, Holder<Biome> smallIslandsBiome, Holder<Biome> barrensBiome) {
        super(ImmutableList.of(centerBiome, highlandsBiome, midlandsBiome, smallIslandsBiome, barrensBiome));
        this.end = centerBiome;
        this.highlands = highlandsBiome;
        this.midlands = midlandsBiome;
        this.islands = smallIslandsBiome;
        this.barrens = barrensBiome;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        int i = QuartPos.toBlock(x);
        int j = QuartPos.toBlock(y);
        int k = QuartPos.toBlock(z);
        int l = SectionPos.blockToSectionCoord(i);
        int m = SectionPos.blockToSectionCoord(k);
        if ((long)l * (long)l + (long)m * (long)m <= 4096L) {
            return this.end;
        } else {
            int n = (SectionPos.blockToSectionCoord(i) * 2 + 1) * 8;
            int o = (SectionPos.blockToSectionCoord(k) * 2 + 1) * 8;
            double d = noise.erosion().compute(new DensityFunction.SinglePointContext(n, j, o));
            if (d > 0.25D) {
                return this.highlands;
            } else if (d >= -0.0625D) {
                return this.midlands;
            } else {
                return d < -0.21875D ? this.islands : this.barrens;
            }
        }
    }
}
