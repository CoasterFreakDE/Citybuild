package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public class MultifaceSpreader {
    public static final MultifaceSpreader.SpreadType[] DEFAULT_SPREAD_ORDER = new MultifaceSpreader.SpreadType[]{MultifaceSpreader.SpreadType.SAME_POSITION, MultifaceSpreader.SpreadType.SAME_PLANE, MultifaceSpreader.SpreadType.WRAP_AROUND};
    private final MultifaceSpreader.SpreadConfig config;

    public MultifaceSpreader(MultifaceBlock lichen) {
        this(new MultifaceSpreader.DefaultSpreaderConfig(lichen));
    }

    public MultifaceSpreader(MultifaceSpreader.SpreadConfig growChecker) {
        this.config = growChecker;
    }

    public boolean canSpreadInAnyDirection(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return Direction.stream().anyMatch((direction2) -> {
            return this.getSpreadFromFaceTowardDirection(state, world, pos, direction, direction2, this.config::canSpreadInto).isPresent();
        });
    }

    public Optional<MultifaceSpreader.SpreadPos> spreadFromRandomFaceTowardRandomDirection(BlockState state, LevelAccessor world, BlockPos pos, RandomSource random) {
        return Direction.allShuffled(random).stream().filter((direction) -> {
            return this.config.canSpreadFrom(state, direction);
        }).map((direction) -> {
            return this.spreadFromFaceTowardRandomDirection(state, world, pos, direction, random, false);
        }).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    public long spreadAll(BlockState state, LevelAccessor world, BlockPos pos, boolean markForPostProcessing) {
        return Direction.stream().filter((direction) -> {
            return this.config.canSpreadFrom(state, direction);
        }).map((direction) -> {
            return this.spreadFromFaceTowardAllDirections(state, world, pos, direction, markForPostProcessing);
        }).reduce(0L, Long::sum);
    }

    public Optional<MultifaceSpreader.SpreadPos> spreadFromFaceTowardRandomDirection(BlockState state, LevelAccessor world, BlockPos pos, Direction direction, RandomSource random, boolean markForPostProcessing) {
        return Direction.allShuffled(random).stream().map((direction2) -> {
            return this.spreadFromFaceTowardDirection(state, world, pos, direction, direction2, markForPostProcessing);
        }).filter(Optional::isPresent).findFirst().orElse(Optional.empty());
    }

    private long spreadFromFaceTowardAllDirections(BlockState state, LevelAccessor world, BlockPos pos, Direction direction, boolean markForPostProcessing) {
        return Direction.stream().map((direction2) -> {
            return this.spreadFromFaceTowardDirection(state, world, pos, direction, direction2, markForPostProcessing);
        }).filter(Optional::isPresent).count();
    }

    @VisibleForTesting
    public Optional<MultifaceSpreader.SpreadPos> spreadFromFaceTowardDirection(BlockState state, LevelAccessor world, BlockPos pos, Direction oldDirection, Direction newDirection, boolean markForPostProcessing) {
        return this.getSpreadFromFaceTowardDirection(state, world, pos, oldDirection, newDirection, this.config::canSpreadInto).flatMap((growPos) -> {
            return this.spreadToFace(world, growPos, markForPostProcessing);
        });
    }

    public Optional<MultifaceSpreader.SpreadPos> getSpreadFromFaceTowardDirection(BlockState state, BlockGetter world, BlockPos pos, Direction oldDirection, Direction newDirection, MultifaceSpreader.SpreadPredicate predicate) {
        if (newDirection.getAxis() == oldDirection.getAxis()) {
            return Optional.empty();
        } else if (this.config.isOtherBlockValidAsSource(state) || this.config.hasFace(state, oldDirection) && !this.config.hasFace(state, newDirection)) {
            for(MultifaceSpreader.SpreadType spreadType : this.config.getSpreadTypes()) {
                MultifaceSpreader.SpreadPos spreadPos = spreadType.getSpreadPos(pos, newDirection, oldDirection);
                if (predicate.test(world, pos, spreadPos)) {
                    return Optional.of(spreadPos);
                }
            }

            return Optional.empty();
        } else {
            return Optional.empty();
        }
    }

    public Optional<MultifaceSpreader.SpreadPos> spreadToFace(LevelAccessor world, MultifaceSpreader.SpreadPos pos, boolean markForPostProcessing) {
        BlockState blockState = world.getBlockState(pos.pos());
        return this.config.placeBlock(world, pos, blockState, markForPostProcessing) ? Optional.of(pos) : Optional.empty();
    }

    public static class DefaultSpreaderConfig implements MultifaceSpreader.SpreadConfig {
        protected MultifaceBlock block;

        public DefaultSpreaderConfig(MultifaceBlock lichen) {
            this.block = lichen;
        }

        @Nullable
        @Override
        public BlockState getStateForPlacement(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
            return this.block.getStateForPlacement(state, world, pos, direction);
        }

        protected boolean stateCanBeReplaced(BlockGetter world, BlockPos pos, BlockPos growPos, Direction direction, BlockState state) {
            return state.isAir() || state.is(this.block) || state.is(Blocks.WATER) && state.getFluidState().isSource();
        }

        @Override
        public boolean canSpreadInto(BlockGetter world, BlockPos pos, MultifaceSpreader.SpreadPos growPos) {
            BlockState blockState = world.getBlockState(growPos.pos());
            return this.stateCanBeReplaced(world, pos, growPos.pos(), growPos.face(), blockState) && this.block.isValidStateForPlacement(world, blockState, growPos.pos(), growPos.face());
        }
    }

    public interface SpreadConfig {
        @Nullable
        BlockState getStateForPlacement(BlockState state, BlockGetter world, BlockPos pos, Direction direction);

        boolean canSpreadInto(BlockGetter world, BlockPos pos, MultifaceSpreader.SpreadPos growPos);

        default MultifaceSpreader.SpreadType[] getSpreadTypes() {
            return MultifaceSpreader.DEFAULT_SPREAD_ORDER;
        }

        default boolean hasFace(BlockState state, Direction direction) {
            return MultifaceBlock.hasFace(state, direction);
        }

        default boolean isOtherBlockValidAsSource(BlockState state) {
            return false;
        }

        default boolean canSpreadFrom(BlockState state, Direction direction) {
            return this.isOtherBlockValidAsSource(state) || this.hasFace(state, direction);
        }

        default boolean placeBlock(LevelAccessor world, MultifaceSpreader.SpreadPos growPos, BlockState state, boolean markForPostProcessing) {
            BlockState blockState = this.getStateForPlacement(state, world, growPos.pos(), growPos.face());
            if (blockState != null) {
                if (markForPostProcessing) {
                    world.getChunk(growPos.pos()).markPosForPostprocessing(growPos.pos());
                }

                return world.setBlock(growPos.pos(), blockState, 2);
            } else {
                return false;
            }
        }
    }

    public static record SpreadPos(BlockPos pos, Direction face) {
    }

    @FunctionalInterface
    public interface SpreadPredicate {
        boolean test(BlockGetter world, BlockPos pos, MultifaceSpreader.SpreadPos growPos);
    }

    public static enum SpreadType {
        SAME_POSITION {
            @Override
            public MultifaceSpreader.SpreadPos getSpreadPos(BlockPos pos, Direction newDirection, Direction oldDirection) {
                return new MultifaceSpreader.SpreadPos(pos, newDirection);
            }
        },
        SAME_PLANE {
            @Override
            public MultifaceSpreader.SpreadPos getSpreadPos(BlockPos pos, Direction newDirection, Direction oldDirection) {
                return new MultifaceSpreader.SpreadPos(pos.relative(newDirection), oldDirection);
            }
        },
        WRAP_AROUND {
            @Override
            public MultifaceSpreader.SpreadPos getSpreadPos(BlockPos pos, Direction newDirection, Direction oldDirection) {
                return new MultifaceSpreader.SpreadPos(pos.relative(newDirection).relative(oldDirection), newDirection.getOpposite());
            }
        };

        public abstract MultifaceSpreader.SpreadPos getSpreadPos(BlockPos pos, Direction newDirection, Direction oldDirection);
    }
}
