package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

public enum RandomSpreadType implements StringRepresentable {
    LINEAR("linear"),
    TRIANGULAR("triangular");

    public static final Codec<RandomSpreadType> CODEC = StringRepresentable.fromEnum(RandomSpreadType::values);
    private final String id;

    private RandomSpreadType(String name) {
        this.id = name;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public int evaluate(RandomSource random, int bound) {
        int var10000;
        switch (this) {
            case LINEAR:
                var10000 = random.nextInt(bound);
                break;
            case TRIANGULAR:
                var10000 = (random.nextInt(bound) + random.nextInt(bound)) / 2;
                break;
            default:
                throw new IncompatibleClassChangeError();
        }

        return var10000;
    }
}
