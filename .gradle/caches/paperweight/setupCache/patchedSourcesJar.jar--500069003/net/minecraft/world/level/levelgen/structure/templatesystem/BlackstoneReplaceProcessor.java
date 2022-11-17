package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BlackstoneReplaceProcessor extends StructureProcessor {
    public static final Codec<BlackstoneReplaceProcessor> CODEC = Codec.unit(() -> {
        return BlackstoneReplaceProcessor.INSTANCE;
    });
    public static final BlackstoneReplaceProcessor INSTANCE = new BlackstoneReplaceProcessor();
    private final Map<Block, Block> replacements = Util.make(Maps.newHashMap(), (replacements) -> {
        replacements.put(Blocks.COBBLESTONE, Blocks.BLACKSTONE);
        replacements.put(Blocks.MOSSY_COBBLESTONE, Blocks.BLACKSTONE);
        replacements.put(Blocks.STONE, Blocks.POLISHED_BLACKSTONE);
        replacements.put(Blocks.STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
        replacements.put(Blocks.MOSSY_STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
        replacements.put(Blocks.COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
        replacements.put(Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
        replacements.put(Blocks.STONE_STAIRS, Blocks.POLISHED_BLACKSTONE_STAIRS);
        replacements.put(Blocks.STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
        replacements.put(Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
        replacements.put(Blocks.COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
        replacements.put(Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.BLACKSTONE_SLAB);
        replacements.put(Blocks.SMOOTH_STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
        replacements.put(Blocks.STONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
        replacements.put(Blocks.STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
        replacements.put(Blocks.MOSSY_STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
        replacements.put(Blocks.STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        replacements.put(Blocks.MOSSY_STONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        replacements.put(Blocks.COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
        replacements.put(Blocks.MOSSY_COBBLESTONE_WALL, Blocks.BLACKSTONE_WALL);
        replacements.put(Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE);
        replacements.put(Blocks.CRACKED_STONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        replacements.put(Blocks.IRON_BARS, Blocks.CHAIN);
    });

    private BlackstoneReplaceProcessor() {
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo, StructurePlaceSettings data) {
        Block block = this.replacements.get(currentBlockInfo.state.getBlock());
        if (block == null) {
            return currentBlockInfo;
        } else {
            BlockState blockState = currentBlockInfo.state;
            BlockState blockState2 = block.defaultBlockState();
            if (blockState.hasProperty(StairBlock.FACING)) {
                blockState2 = blockState2.setValue(StairBlock.FACING, blockState.getValue(StairBlock.FACING));
            }

            if (blockState.hasProperty(StairBlock.HALF)) {
                blockState2 = blockState2.setValue(StairBlock.HALF, blockState.getValue(StairBlock.HALF));
            }

            if (blockState.hasProperty(SlabBlock.TYPE)) {
                blockState2 = blockState2.setValue(SlabBlock.TYPE, blockState.getValue(SlabBlock.TYPE));
            }

            return new StructureTemplate.StructureBlockInfo(currentBlockInfo.pos, blockState2, currentBlockInfo.nbt);
        }
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.BLACKSTONE_REPLACE;
    }
}
