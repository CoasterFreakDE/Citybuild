package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class LinearPosTest extends PosRuleTest {
    public static final Codec<LinearPosTest> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("min_chance").orElse(0.0F).forGetter((ruleTest) -> {
            return ruleTest.minChance;
        }), Codec.FLOAT.fieldOf("max_chance").orElse(0.0F).forGetter((ruleTest) -> {
            return ruleTest.maxChance;
        }), Codec.INT.fieldOf("min_dist").orElse(0).forGetter((ruleTest) -> {
            return ruleTest.minDist;
        }), Codec.INT.fieldOf("max_dist").orElse(0).forGetter((ruleTest) -> {
            return ruleTest.maxDist;
        })).apply(instance, LinearPosTest::new);
    });
    private final float minChance;
    private final float maxChance;
    private final int minDist;
    private final int maxDist;

    public LinearPosTest(float minChance, float maxChance, int minDistance, int maxDistance) {
        if (minDistance >= maxDistance) {
            throw new IllegalArgumentException("Invalid range: [" + minDistance + "," + maxDistance + "]");
        } else {
            this.minChance = minChance;
            this.maxChance = maxChance;
            this.minDist = minDistance;
            this.maxDist = maxDistance;
        }
    }

    @Override
    public boolean test(BlockPos blockPos, BlockPos blockPos2, BlockPos pivot, RandomSource random) {
        int i = blockPos2.distManhattan(pivot);
        float f = random.nextFloat();
        return f <= Mth.clampedLerp(this.minChance, this.maxChance, Mth.inverseLerp((float)i, (float)this.minDist, (float)this.maxDist));
    }

    @Override
    protected PosRuleTestType<?> getType() {
        return PosRuleTestType.LINEAR_POS_TEST;
    }
}
