package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BonemealableBlock {
    boolean isValidBonemealTarget(BlockGetter world, BlockPos pos, BlockState state, boolean isClient);

    boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state);

    void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state);
}
