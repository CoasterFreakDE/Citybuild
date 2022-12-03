package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public class RuleProcessor extends StructureProcessor {
    public static final Codec<RuleProcessor> CODEC = ProcessorRule.CODEC.listOf().fieldOf("rules").xmap(RuleProcessor::new, (processor) -> {
        return processor.rules;
    }).codec();
    private final ImmutableList<ProcessorRule> rules;

    public RuleProcessor(List<? extends ProcessorRule> rules) {
        this.rules = ImmutableList.copyOf(rules);
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo, StructurePlaceSettings data) {
        RandomSource randomSource = RandomSource.create(Mth.getSeed(currentBlockInfo.pos));
        BlockState blockState = world.getBlockState(currentBlockInfo.pos);

        for(ProcessorRule processorRule : this.rules) {
            if (processorRule.test(currentBlockInfo.state, blockState, originalBlockInfo.pos, currentBlockInfo.pos, pivot, randomSource)) {
                return new StructureTemplate.StructureBlockInfo(currentBlockInfo.pos, processorRule.getOutputState(), processorRule.getOutputTag());
            }
        }

        return currentBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.RULE;
    }
}
