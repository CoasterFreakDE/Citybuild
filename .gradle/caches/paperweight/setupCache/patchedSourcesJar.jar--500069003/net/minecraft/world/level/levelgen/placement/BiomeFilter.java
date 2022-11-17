package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public class BiomeFilter extends PlacementFilter {
    private static final BiomeFilter INSTANCE = new BiomeFilter();
    public static Codec<BiomeFilter> CODEC = Codec.unit(() -> {
        return INSTANCE;
    });

    private BiomeFilter() {
    }

    public static BiomeFilter biome() {
        return INSTANCE;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        PlacedFeature placedFeature = context.topFeature().orElseThrow(() -> {
            return new IllegalStateException("Tried to biome check an unregistered feature, or a feature that should not restrict the biome");
        });
        Holder<Biome> holder = context.getLevel().getBiome(pos);
        return context.generator().getBiomeGenerationSettings(holder).hasFeature(placedFeature);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.BIOME_FILTER;
    }
}
