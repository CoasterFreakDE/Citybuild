package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

public class GravityProcessor extends StructureProcessor {
    public static final Codec<GravityProcessor> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Heightmap.Types.CODEC.fieldOf("heightmap").orElse(Heightmap.Types.WORLD_SURFACE_WG).forGetter((processor) -> {
            return processor.heightmap;
        }), Codec.INT.fieldOf("offset").orElse(0).forGetter((processor) -> {
            return processor.offset;
        })).apply(instance, GravityProcessor::new);
    });
    private final Heightmap.Types heightmap;
    private final int offset;

    public GravityProcessor(Heightmap.Types heightmap, int offset) {
        this.heightmap = heightmap;
        this.offset = offset;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo, StructurePlaceSettings data) {
        Heightmap.Types types;
        if (world instanceof ServerLevel) {
            if (this.heightmap == Heightmap.Types.WORLD_SURFACE_WG) {
                types = Heightmap.Types.WORLD_SURFACE;
            } else if (this.heightmap == Heightmap.Types.OCEAN_FLOOR_WG) {
                types = Heightmap.Types.OCEAN_FLOOR;
            } else {
                types = this.heightmap;
            }
        } else {
            types = this.heightmap;
        }

        int i = world.getHeight(types, currentBlockInfo.pos.getX(), currentBlockInfo.pos.getZ()) + this.offset;
        int j = originalBlockInfo.pos.getY();
        return new StructureTemplate.StructureBlockInfo(new BlockPos(currentBlockInfo.pos.getX(), i + j, currentBlockInfo.pos.getZ()), currentBlockInfo.state, currentBlockInfo.nbt);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.GRAVITY;
    }
}
