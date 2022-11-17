package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class HugeFungusConfiguration implements FeatureConfiguration {
    public static final Codec<HugeFungusConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockState.CODEC.fieldOf("valid_base_block").forGetter((config) -> {
            return config.validBaseState;
        }), BlockState.CODEC.fieldOf("stem_state").forGetter((config) -> {
            return config.stemState;
        }), BlockState.CODEC.fieldOf("hat_state").forGetter((config) -> {
            return config.hatState;
        }), BlockState.CODEC.fieldOf("decor_state").forGetter((config) -> {
            return config.decorState;
        }), Codec.BOOL.fieldOf("planted").orElse(false).forGetter((config) -> {
            return config.planted;
        })).apply(instance, HugeFungusConfiguration::new);
    });
    public final BlockState validBaseState;
    public final BlockState stemState;
    public final BlockState hatState;
    public final BlockState decorState;
    public final boolean planted;

    public HugeFungusConfiguration(BlockState validBaseBlock, BlockState stemState, BlockState hatState, BlockState decorationState, boolean planted) {
        this.validBaseState = validBaseBlock;
        this.stemState = stemState;
        this.hatState = hatState;
        this.decorState = decorationState;
        this.planted = planted;
    }
}
