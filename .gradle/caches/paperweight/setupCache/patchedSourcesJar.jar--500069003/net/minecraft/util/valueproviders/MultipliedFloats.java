package net.minecraft.util.valueproviders;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

public class MultipliedFloats implements SampledFloat {
    private final SampledFloat[] values;

    public MultipliedFloats(SampledFloat... multipliers) {
        this.values = multipliers;
    }

    @Override
    public float sample(RandomSource random) {
        float f = 1.0F;

        for(int i = 0; i < this.values.length; ++i) {
            f *= this.values[i].sample(random);
        }

        return f;
    }

    @Override
    public String toString() {
        return "MultipliedFloats" + Arrays.toString((Object[])this.values);
    }
}
