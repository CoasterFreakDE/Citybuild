package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public abstract class NoiseBasedStateProvider extends BlockStateProvider {
    protected final long seed;
    protected final NormalNoise.NoiseParameters parameters;
    protected final float scale;
    protected final NormalNoise noise;

    protected static <P extends NoiseBasedStateProvider> Products.P3<RecordCodecBuilder.Mu<P>, Long, NormalNoise.NoiseParameters, Float> noiseCodec(RecordCodecBuilder.Instance<P> instance) {
        return instance.group(Codec.LONG.fieldOf("seed").forGetter((noiseBasedStateProvider) -> {
            return noiseBasedStateProvider.seed;
        }), NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("noise").forGetter((noiseBasedStateProvider) -> {
            return noiseBasedStateProvider.parameters;
        }), ExtraCodecs.POSITIVE_FLOAT.fieldOf("scale").forGetter((noiseBasedStateProvider) -> {
            return noiseBasedStateProvider.scale;
        }));
    }

    protected NoiseBasedStateProvider(long seed, NormalNoise.NoiseParameters noiseParameters, float scale) {
        this.seed = seed;
        this.parameters = noiseParameters;
        this.scale = scale;
        this.noise = NormalNoise.create(new WorldgenRandom(new LegacyRandomSource(seed)), noiseParameters);
    }

    protected double getNoiseValue(BlockPos pos, double scale) {
        return this.noise.getValue((double)pos.getX() * scale, (double)pos.getY() * scale, (double)pos.getZ() * scale);
    }
}
