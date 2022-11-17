package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;

public class RootSystemFeature extends Feature<RootSystemConfiguration> {
    public RootSystemFeature(Codec<RootSystemConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RootSystemConfiguration> context) {
        WorldGenLevel worldGenLevel = context.level();
        BlockPos blockPos = context.origin();
        if (!worldGenLevel.getBlockState(blockPos).isAir()) {
            return false;
        } else {
            RandomSource randomSource = context.random();
            BlockPos blockPos2 = context.origin();
            RootSystemConfiguration rootSystemConfiguration = context.config();
            BlockPos.MutableBlockPos mutableBlockPos = blockPos2.mutable();
            if (placeDirtAndTree(worldGenLevel, context.chunkGenerator(), rootSystemConfiguration, randomSource, mutableBlockPos, blockPos2)) {
                placeRoots(worldGenLevel, rootSystemConfiguration, randomSource, blockPos2, mutableBlockPos);
            }

            return true;
        }
    }

    private static boolean spaceForTree(WorldGenLevel world, RootSystemConfiguration config, BlockPos pos) {
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

        for(int i = 1; i <= config.requiredVerticalSpaceForTree; ++i) {
            mutableBlockPos.move(Direction.UP);
            BlockState blockState = world.getBlockState(mutableBlockPos);
            if (!isAllowedTreeSpace(blockState, i, config.allowedVerticalWaterForTree)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isAllowedTreeSpace(BlockState state, int height, int allowedVerticalWaterForTree) {
        if (state.isAir()) {
            return true;
        } else {
            int i = height + 1;
            return i <= allowedVerticalWaterForTree && state.getFluidState().is(FluidTags.WATER);
        }
    }

    private static boolean placeDirtAndTree(WorldGenLevel world, ChunkGenerator generator, RootSystemConfiguration config, RandomSource randomSource, BlockPos.MutableBlockPos mutablePos, BlockPos pos) {
        for(int i = 0; i < config.rootColumnMaxHeight; ++i) {
            mutablePos.move(Direction.UP);
            if (config.allowedTreePosition.test(world, mutablePos) && spaceForTree(world, config, mutablePos)) {
                BlockPos blockPos = mutablePos.below();
                if (world.getFluidState(blockPos).is(FluidTags.LAVA) || !world.getBlockState(blockPos).getMaterial().isSolid()) {
                    return false;
                }

                if (config.treeFeature.value().place(world, generator, randomSource, mutablePos)) {
                    placeDirt(pos, pos.getY() + i, world, config, randomSource);
                    return true;
                }
            }
        }

        return false;
    }

    private static void placeDirt(BlockPos pos, int maxY, WorldGenLevel world, RootSystemConfiguration config, RandomSource randomSource) {
        int i = pos.getX();
        int j = pos.getZ();
        BlockPos.MutableBlockPos mutableBlockPos = pos.mutable();

        for(int k = pos.getY(); k < maxY; ++k) {
            placeRootedDirt(world, config, randomSource, i, j, mutableBlockPos.set(i, k, j));
        }

    }

    private static void placeRootedDirt(WorldGenLevel world, RootSystemConfiguration config, RandomSource randomSource, int x, int z, BlockPos.MutableBlockPos mutablePos) {
        int i = config.rootRadius;
        Predicate<BlockState> predicate = (state) -> {
            return state.is(config.rootReplaceable);
        };

        for(int j = 0; j < config.rootPlacementAttempts; ++j) {
            mutablePos.setWithOffset(mutablePos, randomSource.nextInt(i) - randomSource.nextInt(i), 0, randomSource.nextInt(i) - randomSource.nextInt(i));
            if (predicate.test(world.getBlockState(mutablePos))) {
                world.setBlock(mutablePos, config.rootStateProvider.getState(randomSource, mutablePos), 2);
            }

            mutablePos.setX(x);
            mutablePos.setZ(z);
        }

    }

    private static void placeRoots(WorldGenLevel world, RootSystemConfiguration config, RandomSource randomSource, BlockPos pos, BlockPos.MutableBlockPos mutablePos) {
        int i = config.hangingRootRadius;
        int j = config.hangingRootsVerticalSpan;

        for(int k = 0; k < config.hangingRootPlacementAttempts; ++k) {
            mutablePos.setWithOffset(pos, randomSource.nextInt(i) - randomSource.nextInt(i), randomSource.nextInt(j) - randomSource.nextInt(j), randomSource.nextInt(i) - randomSource.nextInt(i));
            if (world.isEmptyBlock(mutablePos)) {
                BlockState blockState = config.hangingRootStateProvider.getState(randomSource, mutablePos);
                if (blockState.canSurvive(world, mutablePos) && world.getBlockState(mutablePos.above()).isFaceSturdy(world, mutablePos, Direction.DOWN)) {
                    world.setBlock(mutablePos, blockState, 2);
                }
            }
        }

    }
}
