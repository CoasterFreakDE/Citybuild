package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class DropExperienceBlock extends Block {
    private final IntProvider xpRange;

    public DropExperienceBlock(BlockBehaviour.Properties settings) {
        this(settings, ConstantInt.of(0));
    }

    public DropExperienceBlock(BlockBehaviour.Properties settings, IntProvider experience) {
        super(settings);
        this.xpRange = experience;
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel world, BlockPos pos, ItemStack stack, boolean dropExperience) {
        super.spawnAfterBreak(state, world, pos, stack, dropExperience);
        if (dropExperience) {
            this.tryDropExperience(world, pos, stack, this.xpRange);
        }

    }
}
