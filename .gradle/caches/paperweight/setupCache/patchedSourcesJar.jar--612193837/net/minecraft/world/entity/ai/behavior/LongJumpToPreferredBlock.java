package net.minecraft.world.entity.ai.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class LongJumpToPreferredBlock<E extends Mob> extends LongJumpToRandomPos<E> {
    private final TagKey<Block> preferredBlockTag;
    private final float preferredBlocksChance;
    private final List<LongJumpToRandomPos.PossibleJump> notPrefferedJumpCandidates = new ArrayList<>();
    private boolean currentlyWantingPreferredOnes;

    public LongJumpToPreferredBlock(UniformInt cooldownRange, int verticalRange, int horizontalRange, float maxRange, Function<E, SoundEvent> entityToSound, TagKey<Block> favoredBlocks, float biasChance, Predicate<BlockState> jumpToPredicate) {
        super(cooldownRange, verticalRange, horizontalRange, maxRange, entityToSound, jumpToPredicate);
        this.preferredBlockTag = favoredBlocks;
        this.preferredBlocksChance = biasChance;
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        super.start(world, entity, time);
        this.notPrefferedJumpCandidates.clear();
        this.currentlyWantingPreferredOnes = entity.getRandom().nextFloat() < this.preferredBlocksChance;
    }

    @Override
    protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel world) {
        if (!this.currentlyWantingPreferredOnes) {
            return super.getJumpCandidate(world);
        } else {
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            while(!this.jumpCandidates.isEmpty()) {
                Optional<LongJumpToRandomPos.PossibleJump> optional = super.getJumpCandidate(world);
                if (optional.isPresent()) {
                    LongJumpToRandomPos.PossibleJump possibleJump = optional.get();
                    if (world.getBlockState(mutableBlockPos.setWithOffset(possibleJump.getJumpTarget(), Direction.DOWN)).is(this.preferredBlockTag)) {
                        return optional;
                    }

                    this.notPrefferedJumpCandidates.add(possibleJump);
                }
            }

            return !this.notPrefferedJumpCandidates.isEmpty() ? Optional.of(this.notPrefferedJumpCandidates.remove(0)) : Optional.empty();
        }
    }

    @Override
    protected boolean isAcceptableLandingPosition(ServerLevel world, E entity, BlockPos pos) {
        return super.isAcceptableLandingPosition(world, entity, pos) && this.willNotLandInFluid(world, pos);
    }

    private boolean willNotLandInFluid(ServerLevel world, BlockPos pos) {
        return world.getFluidState(pos).isEmpty() && world.getFluidState(pos.below()).isEmpty();
    }
}
