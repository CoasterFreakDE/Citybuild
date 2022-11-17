package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;

public class BlockPileFeature extends Feature<BlockPileConfiguration> {
    public BlockPileFeature(Codec<BlockPileConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockPileConfiguration> context) {
        BlockPos blockPos = context.origin();
        WorldGenLevel worldGenLevel = context.level();
        RandomSource randomSource = context.random();
        BlockPileConfiguration blockPileConfiguration = context.config();
        if (blockPos.getY() < worldGenLevel.getMinBuildHeight() + 5) {
            return false;
        } else {
            int i = 2 + randomSource.nextInt(2);
            int j = 2 + randomSource.nextInt(2);

            for(BlockPos blockPos2 : BlockPos.betweenClosed(blockPos.offset(-i, 0, -j), blockPos.offset(i, 1, j))) {
                int k = blockPos.getX() - blockPos2.getX();
                int l = blockPos.getZ() - blockPos2.getZ();
                if ((float)(k * k + l * l) <= randomSource.nextFloat() * 10.0F - randomSource.nextFloat() * 6.0F) {
                    this.tryPlaceBlock(worldGenLevel, blockPos2, randomSource, blockPileConfiguration);
                } else if ((double)randomSource.nextFloat() < 0.031D) {
                    this.tryPlaceBlock(worldGenLevel, blockPos2, randomSource, blockPileConfiguration);
                }
            }

            return true;
        }
    }

    private boolean mayPlaceOn(LevelAccessor world, BlockPos pos, RandomSource random) {
        BlockPos blockPos = pos.below();
        BlockState blockState = world.getBlockState(blockPos);
        return blockState.is(Blocks.DIRT_PATH) ? random.nextBoolean() : blockState.isFaceSturdy(world, blockPos, Direction.UP);
    }

    private void tryPlaceBlock(LevelAccessor world, BlockPos pos, RandomSource random, BlockPileConfiguration config) {
        if (world.isEmptyBlock(pos) && this.mayPlaceOn(world, pos, random)) {
            world.setBlock(pos, config.stateProvider.getState(random, pos), 4);
        }

    }
}
