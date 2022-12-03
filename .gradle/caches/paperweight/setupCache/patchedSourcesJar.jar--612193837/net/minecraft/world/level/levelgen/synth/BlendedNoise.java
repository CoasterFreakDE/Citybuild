package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class BlendedNoise implements DensityFunction.SimpleFunction {
    private static final Codec<Double> SCALE_RANGE = Codec.doubleRange(0.001D, 1000.0D);
    private static final MapCodec<BlendedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(SCALE_RANGE.fieldOf("xz_scale").forGetter((blendedNoise) -> {
            return blendedNoise.xzScale;
        }), SCALE_RANGE.fieldOf("y_scale").forGetter((blendedNoise) -> {
            return blendedNoise.yScale;
        }), SCALE_RANGE.fieldOf("xz_factor").forGetter((blendedNoise) -> {
            return blendedNoise.xzFactor;
        }), SCALE_RANGE.fieldOf("y_factor").forGetter((blendedNoise) -> {
            return blendedNoise.yFactor;
        }), Codec.doubleRange(1.0D, 8.0D).fieldOf("smear_scale_multiplier").forGetter((blendedNoise) -> {
            return blendedNoise.smearScaleMultiplier;
        })).apply(instance, BlendedNoise::createUnseeded);
    });
    public static final KeyDispatchDataCodec<BlendedNoise> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
    private final PerlinNoise minLimitNoise;
    private final PerlinNoise maxLimitNoise;
    private final PerlinNoise mainNoise;
    private final double xzMultiplier;
    private final double yMultiplier;
    private final double xzFactor;
    private final double yFactor;
    private final double smearScaleMultiplier;
    private final double maxValue;
    private final double xzScale;
    private final double yScale;

    public static BlendedNoise createUnseeded(double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier) {
        return new BlendedNoise(new XoroshiroRandomSource(0L), xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier);
    }

    private BlendedNoise(PerlinNoise lowerInterpolatedNoise, PerlinNoise upperInterpolatedNoise, PerlinNoise interpolationNoise, double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier) {
        this.minLimitNoise = lowerInterpolatedNoise;
        this.maxLimitNoise = upperInterpolatedNoise;
        this.mainNoise = interpolationNoise;
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.xzFactor = xzFactor;
        this.yFactor = yFactor;
        this.smearScaleMultiplier = smearScaleMultiplier;
        this.xzMultiplier = 684.412D * this.xzScale;
        this.yMultiplier = 684.412D * this.yScale;
        this.maxValue = lowerInterpolatedNoise.maxBrokenValue(this.yMultiplier);
    }

    @VisibleForTesting
    public BlendedNoise(RandomSource random, double xzScale, double yScale, double xzFactor, double yFactor, double smearScaleMultiplier) {
        this(PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-7, 0)), xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier);
    }

    public BlendedNoise withNewRandom(RandomSource random) {
        return new BlendedNoise(random, this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smearScaleMultiplier);
    }

    @Override
    public double compute(DensityFunction.FunctionContext pos) {
        double d = (double)pos.blockX() * this.xzMultiplier;
        double e = (double)pos.blockY() * this.yMultiplier;
        double f = (double)pos.blockZ() * this.xzMultiplier;
        double g = d / this.xzFactor;
        double h = e / this.yFactor;
        double i = f / this.xzFactor;
        double j = this.yMultiplier * this.smearScaleMultiplier;
        double k = j / this.yFactor;
        double l = 0.0D;
        double m = 0.0D;
        double n = 0.0D;
        boolean bl = true;
        double o = 1.0D;

        for(int p = 0; p < 8; ++p) {
            ImprovedNoise improvedNoise = this.mainNoise.getOctaveNoise(p);
            if (improvedNoise != null) {
                n += improvedNoise.noise(PerlinNoise.wrap(g * o), PerlinNoise.wrap(h * o), PerlinNoise.wrap(i * o), k * o, h * o) / o;
            }

            o /= 2.0D;
        }

        double q = (n / 10.0D + 1.0D) / 2.0D;
        boolean bl2 = q >= 1.0D;
        boolean bl3 = q <= 0.0D;
        o = 1.0D;

        for(int r = 0; r < 16; ++r) {
            double s = PerlinNoise.wrap(d * o);
            double t = PerlinNoise.wrap(e * o);
            double u = PerlinNoise.wrap(f * o);
            double v = j * o;
            if (!bl2) {
                ImprovedNoise improvedNoise2 = this.minLimitNoise.getOctaveNoise(r);
                if (improvedNoise2 != null) {
                    l += improvedNoise2.noise(s, t, u, v, e * o) / o;
                }
            }

            if (!bl3) {
                ImprovedNoise improvedNoise3 = this.maxLimitNoise.getOctaveNoise(r);
                if (improvedNoise3 != null) {
                    m += improvedNoise3.noise(s, t, u, v, e * o) / o;
                }
            }

            o /= 2.0D;
        }

        return Mth.clampedLerp(l / 512.0D, m / 512.0D, q) / 128.0D;
    }

    @Override
    public double minValue() {
        return -this.maxValue();
    }

    @Override
    public double maxValue() {
        return this.maxValue;
    }

    @VisibleForTesting
    public void parityConfigString(StringBuilder info) {
        info.append("BlendedNoise{minLimitNoise=");
        this.minLimitNoise.parityConfigString(info);
        info.append(", maxLimitNoise=");
        this.maxLimitNoise.parityConfigString(info);
        info.append(", mainNoise=");
        this.mainNoise.parityConfigString(info);
        info.append(String.format(Locale.ROOT, ", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8", 684.412D, 684.412D, 8.555150000000001D, 4.277575000000001D)).append('}');
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
