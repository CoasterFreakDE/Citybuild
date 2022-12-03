package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class JigsawReplacementProcessor extends StructureProcessor {
    public static final Codec<JigsawReplacementProcessor> CODEC = Codec.unit(() -> {
        return JigsawReplacementProcessor.INSTANCE;
    });
    public static final JigsawReplacementProcessor INSTANCE = new JigsawReplacementProcessor();

    private JigsawReplacementProcessor() {
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo, StructurePlaceSettings data) {
        BlockState blockState = currentBlockInfo.state;
        if (blockState.is(Blocks.JIGSAW)) {
            String string = currentBlockInfo.nbt.getString("final_state");

            BlockState blockState2;
            try {
                BlockStateParser.BlockResult blockResult = BlockStateParser.parseForBlock(Registry.BLOCK, string, true);
                blockState2 = blockResult.blockState();
            } catch (CommandSyntaxException var11) {
                throw new RuntimeException(var11);
            }

            return blockState2.is(Blocks.STRUCTURE_VOID) ? null : new StructureTemplate.StructureBlockInfo(currentBlockInfo.pos, blockState2, (CompoundTag)null);
        } else {
            return currentBlockInfo;
        }
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.JIGSAW_REPLACEMENT;
    }
}
