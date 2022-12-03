package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class JigsawPlacement {
    static final Logger LOGGER = LogUtils.getLogger();

    public static Optional<Structure.GenerationStub> addPieces(Structure.GenerationContext context, Holder<StructureTemplatePool> structurePool, Optional<ResourceLocation> id, int size, BlockPos pos, boolean useExpansionHack, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter) {
        RegistryAccess registryAccess = context.registryAccess();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        StructureTemplateManager structureTemplateManager = context.structureTemplateManager();
        LevelHeightAccessor levelHeightAccessor = context.heightAccessor();
        WorldgenRandom worldgenRandom = context.random();
        Registry<StructureTemplatePool> registry = registryAccess.registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY);
        Rotation rotation = Rotation.getRandom(worldgenRandom);
        StructureTemplatePool structureTemplatePool = structurePool.value();
        StructurePoolElement structurePoolElement = structureTemplatePool.getRandomTemplate(worldgenRandom);
        if (structurePoolElement == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        } else {
            BlockPos blockPos;
            if (id.isPresent()) {
                ResourceLocation resourceLocation = id.get();
                Optional<BlockPos> optional = getRandomNamedJigsaw(structurePoolElement, resourceLocation, pos, rotation, structureTemplateManager, worldgenRandom);
                if (optional.isEmpty()) {
                    LOGGER.error("No starting jigsaw {} found in start pool {}", resourceLocation, structurePool.unwrapKey().get().location());
                    return Optional.empty();
                }

                blockPos = optional.get();
            } else {
                blockPos = pos;
            }

            Vec3i vec3i = blockPos.subtract(pos);
            BlockPos blockPos3 = pos.subtract(vec3i);
            PoolElementStructurePiece poolElementStructurePiece = new PoolElementStructurePiece(structureTemplateManager, structurePoolElement, blockPos3, structurePoolElement.getGroundLevelDelta(), rotation, structurePoolElement.getBoundingBox(structureTemplateManager, blockPos3, rotation));
            BoundingBox boundingBox = poolElementStructurePiece.getBoundingBox();
            int i = (boundingBox.maxX() + boundingBox.minX()) / 2;
            int j = (boundingBox.maxZ() + boundingBox.minZ()) / 2;
            int k;
            if (projectStartToHeightmap.isPresent()) {
                k = pos.getY() + chunkGenerator.getFirstFreeHeight(i, j, projectStartToHeightmap.get(), levelHeightAccessor, context.randomState());
            } else {
                k = blockPos3.getY();
            }

            int m = boundingBox.minY() + poolElementStructurePiece.getGroundLevelDelta();
            poolElementStructurePiece.move(0, k - m, 0);
            int n = k + vec3i.getY();
            return Optional.of(new Structure.GenerationStub(new BlockPos(i, n, j), (collector) -> {
                List<PoolElementStructurePiece> list = Lists.newArrayList();
                list.add(poolElementStructurePiece);
                if (size > 0) {
                    AABB aABB = new AABB((double)(i - maxDistanceFromCenter), (double)(n - maxDistanceFromCenter), (double)(j - maxDistanceFromCenter), (double)(i + maxDistanceFromCenter + 1), (double)(n + maxDistanceFromCenter + 1), (double)(j + maxDistanceFromCenter + 1));
                    VoxelShape voxelShape = Shapes.join(Shapes.create(aABB), Shapes.create(AABB.of(boundingBox)), BooleanOp.ONLY_FIRST);
                    addPieces(context.randomState(), size, useExpansionHack, chunkGenerator, structureTemplateManager, levelHeightAccessor, worldgenRandom, registry, poolElementStructurePiece, list, voxelShape);
                    list.forEach(collector::addPiece);
                }
            }));
        }
    }

    private static Optional<BlockPos> getRandomNamedJigsaw(StructurePoolElement pool, ResourceLocation id, BlockPos pos, Rotation rotation, StructureTemplateManager structureManager, WorldgenRandom random) {
        List<StructureTemplate.StructureBlockInfo> list = pool.getShuffledJigsawBlocks(structureManager, pos, rotation, random);
        Optional<BlockPos> optional = Optional.empty();

        for(StructureTemplate.StructureBlockInfo structureBlockInfo : list) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(structureBlockInfo.nbt.getString("name"));
            if (id.equals(resourceLocation)) {
                optional = Optional.of(structureBlockInfo.pos);
                break;
            }
        }

        return optional;
    }

    private static void addPieces(RandomState noiseConfig, int maxSize, boolean modifyBoundingBox, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, LevelHeightAccessor heightLimitView, RandomSource random, Registry<StructureTemplatePool> structurePoolRegistry, PoolElementStructurePiece firstPiece, List<PoolElementStructurePiece> pieces, VoxelShape pieceShape) {
        JigsawPlacement.Placer placer = new JigsawPlacement.Placer(structurePoolRegistry, maxSize, chunkGenerator, structureTemplateManager, pieces, random);
        placer.placing.addLast(new JigsawPlacement.PieceState(firstPiece, new MutableObject<>(pieceShape), 0));

        while(!placer.placing.isEmpty()) {
            JigsawPlacement.PieceState pieceState = placer.placing.removeFirst();
            placer.tryPlacingChildren(pieceState.piece, pieceState.free, pieceState.depth, modifyBoundingBox, heightLimitView, noiseConfig);
        }

    }

    public static boolean generateJigsaw(ServerLevel world, Holder<StructureTemplatePool> structurePool, ResourceLocation id, int i, BlockPos pos, boolean keepJigsaws) {
        ChunkGenerator chunkGenerator = world.getChunkSource().getGenerator();
        StructureTemplateManager structureTemplateManager = world.getStructureManager();
        StructureManager structureManager = world.structureManager();
        RandomSource randomSource = world.getRandom();
        Structure.GenerationContext generationContext = new Structure.GenerationContext(world.registryAccess(), chunkGenerator, chunkGenerator.getBiomeSource(), world.getChunkSource().randomState(), structureTemplateManager, world.getSeed(), new ChunkPos(pos), world, (holder) -> {
            return true;
        });
        Optional<Structure.GenerationStub> optional = addPieces(generationContext, structurePool, Optional.of(id), i, pos, false, Optional.empty(), 128);
        if (optional.isPresent()) {
            StructurePiecesBuilder structurePiecesBuilder = optional.get().getPiecesBuilder();

            for(StructurePiece structurePiece : structurePiecesBuilder.build().pieces()) {
                if (structurePiece instanceof PoolElementStructurePiece) {
                    PoolElementStructurePiece poolElementStructurePiece = (PoolElementStructurePiece)structurePiece;
                    poolElementStructurePiece.place(world, structureManager, chunkGenerator, randomSource, BoundingBox.infinite(), pos, keepJigsaws);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    static final class PieceState {
        final PoolElementStructurePiece piece;
        final MutableObject<VoxelShape> free;
        final int depth;

        PieceState(PoolElementStructurePiece piece, MutableObject<VoxelShape> pieceShape, int currentSize) {
            this.piece = piece;
            this.free = pieceShape;
            this.depth = currentSize;
        }
    }

    static final class Placer {
        private final Registry<StructureTemplatePool> pools;
        private final int maxDepth;
        private final ChunkGenerator chunkGenerator;
        private final StructureTemplateManager structureTemplateManager;
        private final List<? super PoolElementStructurePiece> pieces;
        private final RandomSource random;
        final Deque<JigsawPlacement.PieceState> placing = Queues.newArrayDeque();

        Placer(Registry<StructureTemplatePool> registry, int maxSize, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, List<? super PoolElementStructurePiece> children, RandomSource random) {
            this.pools = registry;
            this.maxDepth = maxSize;
            this.chunkGenerator = chunkGenerator;
            this.structureTemplateManager = structureTemplateManager;
            this.pieces = children;
            this.random = random;
        }

        void tryPlacingChildren(PoolElementStructurePiece piece, MutableObject<VoxelShape> pieceShape, int minY, boolean modifyBoundingBox, LevelHeightAccessor world, RandomState noiseConfig) {
            StructurePoolElement structurePoolElement = piece.getElement();
            BlockPos blockPos = piece.getPosition();
            Rotation rotation = piece.getRotation();
            StructureTemplatePool.Projection projection = structurePoolElement.getProjection();
            boolean bl = projection == StructureTemplatePool.Projection.RIGID;
            MutableObject<VoxelShape> mutableObject = new MutableObject<>();
            BoundingBox boundingBox = piece.getBoundingBox();
            int i = boundingBox.minY();

            label139:
            for(StructureTemplate.StructureBlockInfo structureBlockInfo : structurePoolElement.getShuffledJigsawBlocks(this.structureTemplateManager, blockPos, rotation, this.random)) {
                Direction direction = JigsawBlock.getFrontFacing(structureBlockInfo.state);
                BlockPos blockPos2 = structureBlockInfo.pos;
                BlockPos blockPos3 = blockPos2.relative(direction);
                int j = blockPos2.getY() - i;
                int k = -1;
                ResourceLocation resourceLocation = new ResourceLocation(structureBlockInfo.nbt.getString("pool"));
                Optional<StructureTemplatePool> optional = this.pools.getOptional(resourceLocation);
                if (optional.isPresent() && (optional.get().size() != 0 || Objects.equals(resourceLocation, Pools.EMPTY.location()))) {
                    ResourceLocation resourceLocation2 = optional.get().getFallback();
                    Optional<StructureTemplatePool> optional2 = this.pools.getOptional(resourceLocation2);
                    if (optional2.isPresent() && (optional2.get().size() != 0 || Objects.equals(resourceLocation2, Pools.EMPTY.location()))) {
                        boolean bl2 = boundingBox.isInside(blockPos3);
                        MutableObject<VoxelShape> mutableObject2;
                        if (bl2) {
                            mutableObject2 = mutableObject;
                            if (mutableObject.getValue() == null) {
                                mutableObject.setValue(Shapes.create(AABB.of(boundingBox)));
                            }
                        } else {
                            mutableObject2 = pieceShape;
                        }

                        List<StructurePoolElement> list = Lists.newArrayList();
                        if (minY != this.maxDepth) {
                            list.addAll(optional.get().getShuffledTemplates(this.random));
                        }

                        list.addAll(optional2.get().getShuffledTemplates(this.random));

                        for(StructurePoolElement structurePoolElement2 : list) {
                            if (structurePoolElement2 == EmptyPoolElement.INSTANCE) {
                                break;
                            }

                            for(Rotation rotation2 : Rotation.getShuffled(this.random)) {
                                List<StructureTemplate.StructureBlockInfo> list2 = structurePoolElement2.getShuffledJigsawBlocks(this.structureTemplateManager, BlockPos.ZERO, rotation2, this.random);
                                BoundingBox boundingBox2 = structurePoolElement2.getBoundingBox(this.structureTemplateManager, BlockPos.ZERO, rotation2);
                                int m;
                                if (modifyBoundingBox && boundingBox2.getYSpan() <= 16) {
                                    m = list2.stream().mapToInt((structureBlockInfox) -> {
                                        if (!boundingBox2.isInside(structureBlockInfox.pos.relative(JigsawBlock.getFrontFacing(structureBlockInfox.state)))) {
                                            return 0;
                                        } else {
                                            ResourceLocation resourceLocation = new ResourceLocation(structureBlockInfox.nbt.getString("pool"));
                                            Optional<StructureTemplatePool> optional = this.pools.getOptional(resourceLocation);
                                            Optional<StructureTemplatePool> optional2 = optional.flatMap((pool) -> {
                                                return this.pools.getOptional(pool.getFallback());
                                            });
                                            int i = optional.map((pool) -> {
                                                return pool.getMaxSize(this.structureTemplateManager);
                                            }).orElse(0);
                                            int j = optional2.map((pool) -> {
                                                return pool.getMaxSize(this.structureTemplateManager);
                                            }).orElse(0);
                                            return Math.max(i, j);
                                        }
                                    }).max().orElse(0);
                                } else {
                                    m = 0;
                                }

                                for(StructureTemplate.StructureBlockInfo structureBlockInfo2 : list2) {
                                    if (JigsawBlock.canAttach(structureBlockInfo, structureBlockInfo2)) {
                                        BlockPos blockPos4 = structureBlockInfo2.pos;
                                        BlockPos blockPos5 = blockPos3.subtract(blockPos4);
                                        BoundingBox boundingBox3 = structurePoolElement2.getBoundingBox(this.structureTemplateManager, blockPos5, rotation2);
                                        int n = boundingBox3.minY();
                                        StructureTemplatePool.Projection projection2 = structurePoolElement2.getProjection();
                                        boolean bl3 = projection2 == StructureTemplatePool.Projection.RIGID;
                                        int o = blockPos4.getY();
                                        int p = j - o + JigsawBlock.getFrontFacing(structureBlockInfo.state).getStepY();
                                        int q;
                                        if (bl && bl3) {
                                            q = i + p;
                                        } else {
                                            if (k == -1) {
                                                k = this.chunkGenerator.getFirstFreeHeight(blockPos2.getX(), blockPos2.getZ(), Heightmap.Types.WORLD_SURFACE_WG, world, noiseConfig);
                                            }

                                            q = k - o;
                                        }

                                        int s = q - n;
                                        BoundingBox boundingBox4 = boundingBox3.moved(0, s, 0);
                                        BlockPos blockPos6 = blockPos5.offset(0, s, 0);
                                        if (m > 0) {
                                            int t = Math.max(m + 1, boundingBox4.maxY() - boundingBox4.minY());
                                            boundingBox4.encapsulate(new BlockPos(boundingBox4.minX(), boundingBox4.minY() + t, boundingBox4.minZ()));
                                        }

                                        if (!Shapes.joinIsNotEmpty(mutableObject2.getValue(), Shapes.create(AABB.of(boundingBox4).deflate(0.25D)), BooleanOp.ONLY_SECOND)) {
                                            mutableObject2.setValue(Shapes.joinUnoptimized(mutableObject2.getValue(), Shapes.create(AABB.of(boundingBox4)), BooleanOp.ONLY_FIRST));
                                            int u = piece.getGroundLevelDelta();
                                            int v;
                                            if (bl3) {
                                                v = u - p;
                                            } else {
                                                v = structurePoolElement2.getGroundLevelDelta();
                                            }

                                            PoolElementStructurePiece poolElementStructurePiece = new PoolElementStructurePiece(this.structureTemplateManager, structurePoolElement2, blockPos6, v, rotation2, boundingBox4);
                                            int x;
                                            if (bl) {
                                                x = i + j;
                                            } else if (bl3) {
                                                x = q + o;
                                            } else {
                                                if (k == -1) {
                                                    k = this.chunkGenerator.getFirstFreeHeight(blockPos2.getX(), blockPos2.getZ(), Heightmap.Types.WORLD_SURFACE_WG, world, noiseConfig);
                                                }

                                                x = k + p / 2;
                                            }

                                            piece.addJunction(new JigsawJunction(blockPos3.getX(), x - j + u, blockPos3.getZ(), p, projection2));
                                            poolElementStructurePiece.addJunction(new JigsawJunction(blockPos2.getX(), x - o + v, blockPos2.getZ(), -p, projection));
                                            this.pieces.add(poolElementStructurePiece);
                                            if (minY + 1 <= this.maxDepth) {
                                                this.placing.addLast(new JigsawPlacement.PieceState(poolElementStructurePiece, mutableObject2, minY + 1));
                                            }
                                            continue label139;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        JigsawPlacement.LOGGER.warn("Empty or non-existent fallback pool: {}", (Object)resourceLocation2);
                    }
                } else {
                    JigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", (Object)resourceLocation);
                }
            }

        }
    }
}
