package net.minecraft.world.level.redstone;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface NeighborUpdater {
    Direction[] UPDATE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};

    void shapeUpdate(Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, int flags, int maxUpdateDepth);

    void neighborChanged(BlockPos pos, Block sourceBlock, BlockPos sourcePos);

    void neighborChanged(BlockState state, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify);

    default void updateNeighborsAtExceptFromFacing(BlockPos pos, Block sourceBlock, @Nullable Direction except) {
        for(Direction direction : UPDATE_ORDER) {
            if (direction != except) {
                this.neighborChanged(pos.relative(direction), sourceBlock, pos);
            }
        }

    }

    static void executeShapeUpdate(LevelAccessor world, Direction direction, BlockState neighborState, BlockPos pos, BlockPos neighborPos, int flags, int maxUpdateDepth) {
        BlockState blockState = world.getBlockState(pos);
        BlockState blockState2 = blockState.updateShape(direction, neighborState, world, pos, neighborPos);
        Block.updateOrDestroy(blockState, blockState2, world, pos, flags, maxUpdateDepth);
    }

    static void executeUpdate(Level world, BlockState state, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        try {
            state.neighborChanged(world, pos, sourceBlock, sourcePos, notify);
        } catch (Throwable var9) {
            CrashReport crashReport = CrashReport.forThrowable(var9, "Exception while updating neighbours");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being updated");
            crashReportCategory.setDetail("Source block type", () -> {
                try {
                    return String.format(Locale.ROOT, "ID #%s (%s // %s)", Registry.BLOCK.getKey(sourceBlock), sourceBlock.getDescriptionId(), sourceBlock.getClass().getCanonicalName());
                } catch (Throwable var2) {
                    return "ID #" + Registry.BLOCK.getKey(sourceBlock);
                }
            });
            CrashReportCategory.populateBlockDetails(crashReportCategory, world, pos, state);
            throw new ReportedException(crashReport);
        }
    }
}
