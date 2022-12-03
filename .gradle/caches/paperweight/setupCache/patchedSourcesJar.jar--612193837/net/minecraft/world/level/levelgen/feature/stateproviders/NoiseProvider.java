package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseProvider extends NoiseBasedStateProvider {
    public static final Codec<NoiseProvider> CODEC = RecordCodecBuilder.create((instance) -> {
        return noiseProviderCodec(instance).apply(instance, NoiseProvider::new);
    });
    protected final List<BlockState> states;

    protected static <P extends NoiseProvider> Products.P4<RecordCodecBuilder.Mu<P>, Long, NormalNoise.NoiseParameters, Float, List<BlockState>> noiseProviderCodec(RecordCodecBuilder.Instance<P> instance) {
        return noiseCodec(instance).and(Codec.list(BlockState.CODEC).fieldOf("states").forGetter((noiseProvider) -> {
            return noiseProvider.states;
        }));
    }

    public NoiseProvider(long seed, NormalNoise.NoiseParameters noiseParameters, float scale, List<BlockState> states) {
        super(seed, noiseParameters, scale);
        this.states = states;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.NOISE_PROVIDER;
    }

    @Override
    public BlockState getState(RandomSource random, BlockPos pos) {
        return this.getRandomState(this.states, pos, (double)this.scale);
    }

    protected BlockState getRandomState(List<BlockState> states, BlockPos pos, double scale) {
        double d = this.getNoiseValue(pos, scale);
        return this.getRandomState(states, d);
    }

    protected BlockState getRandomState(List<BlockState> states, double value) {
        double d = Mth.clamp((1.0D + value) / 2.0D, 0.0D, 0.9999D);
        return states.get((int)(d * (double)states.size()));
    }
}
