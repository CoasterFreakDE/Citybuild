package net.minecraft.world.level.block;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;

public class SculkVeinBlock extends MultifaceBlock implements SculkBehaviour, SimpleWaterloggedBlock {
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final MultifaceSpreader veinSpreader = new MultifaceSpreader(new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.DEFAULT_SPREAD_ORDER));
    private final MultifaceSpreader sameSpaceSpreader = new MultifaceSpreader(new SculkVeinBlock.SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType.SAME_POSITION));

    public SculkVeinBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, Boolean.valueOf(false)));
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return this.veinSpreader;
    }

    public MultifaceSpreader getSameSpaceSpreader() {
        return this.sameSpaceSpreader;
    }

    public static boolean regrow(LevelAccessor world, BlockPos pos, BlockState state, Collection<Direction> directions) {
        boolean bl = false;
        BlockState blockState = Blocks.SCULK_VEIN.defaultBlockState();

        for(Direction direction : directions) {
            BlockPos blockPos = pos.relative(direction);
            if (canAttachTo(world, direction, blockPos, world.getBlockState(blockPos))) {
                blockState = blockState.setValue(getFaceProperty(direction), Boolean.valueOf(true));
                bl = true;
            }
        }

        if (!bl) {
            return false;
        } else {
            if (!state.getFluidState().isEmpty()) {
                blockState = blockState.setValue(WATERLOGGED, Boolean.valueOf(true));
            }

            world.setBlock(pos, blockState, 3);
            return true;
        }
    }

    @Override
    public void onDischarged(LevelAccessor world, BlockState state, BlockPos pos, RandomSource random) {
        if (state.is(this)) {
            for(Direction direction : DIRECTIONS) {
                BooleanProperty booleanProperty = getFaceProperty(direction);
                if (state.getValue(booleanProperty) && world.getBlockState(pos.relative(direction)).is(Blocks.SCULK)) {
                    state = state.setValue(booleanProperty, Boolean.valueOf(false));
                }
            }

            if (!hasAnyFace(state)) {
                FluidState fluidState = world.getFluidState(pos);
                state = (fluidState.isEmpty() ? Blocks.AIR : Blocks.WATER).defaultBlockState();
            }

            world.setBlock(pos, state, 3);
            SculkBehaviour.super.onDischarged(world, state, pos, random);
        }
    }

    @Override
    public int attemptUseCharge(SculkSpreader.ChargeCursor cursor, LevelAccessor world, BlockPos catalystPos, RandomSource random, SculkSpreader spreadManager, boolean shouldConvertToBlock) {
        if (shouldConvertToBlock && this.attemptPlaceSculk(spreadManager, world, cursor.getPos(), random)) {
            return cursor.getCharge() - 1;
        } else {
            return random.nextInt(spreadManager.chargeDecayRate()) == 0 ? Mth.floor((float)cursor.getCharge() * 0.5F) : cursor.getCharge();
        }
    }

    private boolean attemptPlaceSculk(SculkSpreader spreadManager, LevelAccessor world, BlockPos pos, RandomSource random) {
        BlockState blockState = world.getBlockState(pos);
        TagKey<Block> tagKey = spreadManager.replaceableBlocks();

        for(Direction direction : Direction.allShuffled(random)) {
            if (hasFace(blockState, direction)) {
                BlockPos blockPos = pos.relative(direction);
                BlockState blockState2 = world.getBlockState(blockPos);
                if (blockState2.is(tagKey)) {
                    BlockState blockState3 = Blocks.SCULK.defaultBlockState();
                    world.setBlock(blockPos, blockState3, 3);
                    Block.pushEntitiesUp(blockState2, blockState3, world, blockPos);
                    world.playSound((Player)null, blockPos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 1.0F);
                    this.veinSpreader.spreadAll(blockState3, world, blockPos, spreadManager.isWorldGeneration());
                    Direction direction2 = direction.getOpposite();

                    for(Direction direction3 : DIRECTIONS) {
                        if (direction3 != direction2) {
                            BlockPos blockPos2 = blockPos.relative(direction3);
                            BlockState blockState4 = world.getBlockState(blockPos2);
                            if (blockState4.is(this)) {
                                this.onDischarged(world, blockState4, blockPos2, random);
                            }
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public static boolean hasSubstrateAccess(LevelAccessor world, BlockState state, BlockPos pos) {
        if (!state.is(Blocks.SCULK_VEIN)) {
            return false;
        } else {
            for(Direction direction : DIRECTIONS) {
                if (hasFace(state, direction) && world.getBlockState(pos.relative(direction)).is(BlockTags.SCULK_REPLACEABLE)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.getItemInHand().is(Items.SCULK_VEIN) || super.canBeReplaced(state, context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    class SculkVeinSpreaderConfig extends MultifaceSpreader.DefaultSpreaderConfig {
        private final MultifaceSpreader.SpreadType[] spreadTypes;

        public SculkVeinSpreaderConfig(MultifaceSpreader.SpreadType... growTypes) {
            super(SculkVeinBlock.this);
            this.spreadTypes = growTypes;
        }

        @Override
        public boolean stateCanBeReplaced(BlockGetter world, BlockPos pos, BlockPos growPos, Direction direction, BlockState state) {
            BlockState blockState = world.getBlockState(growPos.relative(direction));
            if (!blockState.is(Blocks.SCULK) && !blockState.is(Blocks.SCULK_CATALYST) && !blockState.is(Blocks.MOVING_PISTON)) {
                if (pos.distManhattan(growPos) == 2) {
                    BlockPos blockPos = pos.relative(direction.getOpposite());
                    if (world.getBlockState(blockPos).isFaceSturdy(world, blockPos, direction)) {
                        return false;
                    }
                }

                FluidState fluidState = state.getFluidState();
                if (!fluidState.isEmpty() && !fluidState.is(Fluids.WATER)) {
                    return false;
                } else {
                    Material material = state.getMaterial();
                    if (material == Material.FIRE) {
                        return false;
                    } else {
                        return material.isReplaceable() || super.stateCanBeReplaced(world, pos, growPos, direction, state);
                    }
                }
            } else {
                return false;
            }
        }

        @Override
        public MultifaceSpreader.SpreadType[] getSpreadTypes() {
            return this.spreadTypes;
        }

        @Override
        public boolean isOtherBlockValidAsSource(BlockState state) {
            return !state.is(Blocks.SCULK_VEIN);
        }
    }
}
