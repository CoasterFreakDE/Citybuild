package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

public class BlockRotProcessor extends StructureProcessor {
    public static final Codec<BlockRotProcessor> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryCodecs.homogeneousList(Registry.BLOCK_REGISTRY).optionalFieldOf("rottable_blocks").forGetter((processor) -> {
            return processor.rottableBlocks;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("integrity").forGetter((processor) -> {
            return processor.integrity;
        })).apply(instance, BlockRotProcessor::new);
    });
    private Optional<HolderSet<Block>> rottableBlocks;
    private final float integrity;

    public BlockRotProcessor(TagKey<Block> rottableBlocks, float integrity) {
        this(Optional.of(Registry.BLOCK.getOrCreateTag(rottableBlocks)), integrity);
    }

    public BlockRotProcessor(float integrity) {
        this(Optional.empty(), integrity);
    }

    private BlockRotProcessor(Optional<HolderSet<Block>> rottableBlocks, float integrity) {
        this.integrity = integrity;
        this.rottableBlocks = rottableBlocks;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo, StructurePlaceSettings data) {
        RandomSource randomSource = data.getRandom(currentBlockInfo.pos);
        return (!this.rottableBlocks.isPresent() || originalBlockInfo.state.is(this.rottableBlocks.get())) && !(randomSource.nextFloat() <= this.integrity) ? null : currentBlockInfo;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.BLOCK_ROT;
    }
}
