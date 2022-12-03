package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public final class JigsawStructure extends Structure {
    public static final int MAX_TOTAL_STRUCTURE_RANGE = 128;
    public static final Codec<JigsawStructure> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(settingsCodec(instance), StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter((structure) -> {
            return structure.startPool;
        }), ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter((structure) -> {
            return structure.startJigsawName;
        }), Codec.intRange(0, 7).fieldOf("size").forGetter((structure) -> {
            return structure.maxDepth;
        }), HeightProvider.CODEC.fieldOf("start_height").forGetter((structure) -> {
            return structure.startHeight;
        }), Codec.BOOL.fieldOf("use_expansion_hack").forGetter((structure) -> {
            return structure.useExpansionHack;
        }), Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter((structure) -> {
            return structure.projectStartToHeightmap;
        }), Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter((structure) -> {
            return structure.maxDistanceFromCenter;
        })).apply(instance, JigsawStructure::new);
    }).flatXmap(verifyRange(), verifyRange()).codec();
    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;

    private static Function<JigsawStructure, DataResult<JigsawStructure>> verifyRange() {
        return (feature) -> {
            byte var10000;
            switch (feature.terrainAdaptation()) {
                case NONE:
                    var10000 = 0;
                    break;
                case BURY:
                case BEARD_THIN:
                case BEARD_BOX:
                    var10000 = 12;
                    break;
                default:
                    throw new IncompatibleClassChangeError();
            }

            int i = var10000;
            return feature.maxDistanceFromCenter + i > 128 ? DataResult.error("Structure size including terrain adaptation must not exceed 128") : DataResult.success(feature);
        };
    }

    public JigsawStructure(Structure.StructureSettings config, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int size, HeightProvider startHeight, boolean useExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter) {
        super(config);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = size;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
    }

    public JigsawStructure(Structure.StructureSettings config, Holder<StructureTemplatePool> startPool, int size, HeightProvider startHeight, boolean useExpansionHack, Heightmap.Types projectStartToHeightmap) {
        this(config, startPool, Optional.empty(), size, startHeight, useExpansionHack, Optional.of(projectStartToHeightmap), 80);
    }

    public JigsawStructure(Structure.StructureSettings config, Holder<StructureTemplatePool> startPool, int size, HeightProvider startHeight, boolean useExpansionHack) {
        this(config, startPool, Optional.empty(), size, startHeight, useExpansionHack, Optional.empty(), 80);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int i = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos blockPos = new BlockPos(chunkPos.getMinBlockX(), i, chunkPos.getMinBlockZ());
        Pools.forceBootstrap();
        return JigsawPlacement.addPieces(context, this.startPool, this.startJigsawName, this.maxDepth, blockPos, this.useExpansionHack, this.projectStartToHeightmap, this.maxDistanceFromCenter);
    }

    @Override
    public StructureType<?> type() {
        return StructureType.JIGSAW;
    }
}
