package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class SculkBlock extends DropExperienceBlock implements SculkBehaviour {
    public SculkBlock(BlockBehaviour.Properties settings) {
        super(settings, ConstantInt.of(1));
    }

    @Override
    public int attemptUseCharge(SculkSpreader.ChargeCursor cursor, LevelAccessor world, BlockPos catalystPos, RandomSource random, SculkSpreader spreadManager, boolean shouldConvertToBlock) {
        int i = cursor.getCharge();
        if (i != 0 && random.nextInt(spreadManager.chargeDecayRate()) == 0) {
            BlockPos blockPos = cursor.getPos();
            boolean bl = blockPos.closerThan(catalystPos, (double)spreadManager.noGrowthRadius());
            if (!bl && canPlaceGrowth(world, blockPos)) {
                int j = spreadManager.growthSpawnCost();
                if (random.nextInt(j) < i) {
                    BlockPos blockPos2 = blockPos.above();
                    BlockState blockState = this.getRandomGrowthState(world, blockPos2, random, spreadManager.isWorldGeneration());
                    world.setBlock(blockPos2, blockState, 3);
                    world.playSound((Player)null, blockPos, blockState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                return Math.max(0, i - j);
            } else {
                return random.nextInt(spreadManager.additionalDecayRate()) != 0 ? i : i - (bl ? 1 : getDecayPenalty(spreadManager, blockPos, catalystPos, i));
            }
        } else {
            return i;
        }
    }

    private static int getDecayPenalty(SculkSpreader spreadManager, BlockPos cursorPos, BlockPos catalystPos, int charge) {
        int i = spreadManager.noGrowthRadius();
        float f = Mth.square((float)Math.sqrt(cursorPos.distSqr(catalystPos)) - (float)i);
        int j = Mth.square(24 - i);
        float g = Math.min(1.0F, f / (float)j);
        return Math.max(1, (int)((float)charge * g * 0.5F));
    }

    private BlockState getRandomGrowthState(LevelAccessor world, BlockPos pos, RandomSource random, boolean allowShrieker) {
        BlockState blockState;
        if (random.nextInt(11) == 0) {
            blockState = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, Boolean.valueOf(allowShrieker));
        } else {
            blockState = Blocks.SCULK_SENSOR.defaultBlockState();
        }

        return blockState.hasProperty(BlockStateProperties.WATERLOGGED) && !world.getFluidState(pos).isEmpty() ? blockState.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true)) : blockState;
    }

    private static boolean canPlaceGrowth(LevelAccessor world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.above());
        if (blockState.isAir() || blockState.is(Blocks.WATER) && blockState.getFluidState().is(Fluids.WATER)) {
            int i = 0;

            for(BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 2, 4))) {
                BlockState blockState2 = world.getBlockState(blockPos);
                if (blockState2.is(Blocks.SCULK_SENSOR) || blockState2.is(Blocks.SCULK_SHRIEKER)) {
                    ++i;
                }

                if (i > 2) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canChangeBlockStateOnSpread() {
        return false;
    }
}
