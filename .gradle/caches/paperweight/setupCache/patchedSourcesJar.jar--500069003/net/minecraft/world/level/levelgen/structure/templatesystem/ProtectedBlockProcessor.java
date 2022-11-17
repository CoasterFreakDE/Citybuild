package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;

public class ProtectedBlockProcessor extends StructureProcessor {
    public final TagKey<Block> cannotReplace;
    public static final Codec<ProtectedBlockProcessor> CODEC = TagKey.hashedCodec(Registry.BLOCK_REGISTRY).xmap(ProtectedBlockProcessor::new, (processor) -> {
        return processor.cannotReplace;
    });

    public ProtectedBlockProcessor(TagKey<Block> protectedBlocksTag) {
        this.cannotReplace = protectedBlocksTag;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo, StructurePlaceSettings data) {
        return Feature.isReplaceable(this.cannotReplace).test(world.getBlockState(currentBlockInfo.pos)) ? currentBlockInfo : null;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.PROTECTED_BLOCKS;
    }
}
