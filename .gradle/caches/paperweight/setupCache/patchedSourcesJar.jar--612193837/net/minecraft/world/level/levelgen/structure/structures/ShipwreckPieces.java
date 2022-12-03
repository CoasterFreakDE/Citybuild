package net.minecraft.world.level.levelgen.structure.structures;

import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class ShipwreckPieces {
    static final BlockPos PIVOT = new BlockPos(4, 0, 15);
    private static final ResourceLocation[] STRUCTURE_LOCATION_BEACHED = new ResourceLocation[]{new ResourceLocation("shipwreck/with_mast"), new ResourceLocation("shipwreck/sideways_full"), new ResourceLocation("shipwreck/sideways_fronthalf"), new ResourceLocation("shipwreck/sideways_backhalf"), new ResourceLocation("shipwreck/rightsideup_full"), new ResourceLocation("shipwreck/rightsideup_fronthalf"), new ResourceLocation("shipwreck/rightsideup_backhalf"), new ResourceLocation("shipwreck/with_mast_degraded"), new ResourceLocation("shipwreck/rightsideup_full_degraded"), new ResourceLocation("shipwreck/rightsideup_fronthalf_degraded"), new ResourceLocation("shipwreck/rightsideup_backhalf_degraded")};
    private static final ResourceLocation[] STRUCTURE_LOCATION_OCEAN = new ResourceLocation[]{new ResourceLocation("shipwreck/with_mast"), new ResourceLocation("shipwreck/upsidedown_full"), new ResourceLocation("shipwreck/upsidedown_fronthalf"), new ResourceLocation("shipwreck/upsidedown_backhalf"), new ResourceLocation("shipwreck/sideways_full"), new ResourceLocation("shipwreck/sideways_fronthalf"), new ResourceLocation("shipwreck/sideways_backhalf"), new ResourceLocation("shipwreck/rightsideup_full"), new ResourceLocation("shipwreck/rightsideup_fronthalf"), new ResourceLocation("shipwreck/rightsideup_backhalf"), new ResourceLocation("shipwreck/with_mast_degraded"), new ResourceLocation("shipwreck/upsidedown_full_degraded"), new ResourceLocation("shipwreck/upsidedown_fronthalf_degraded"), new ResourceLocation("shipwreck/upsidedown_backhalf_degraded"), new ResourceLocation("shipwreck/sideways_full_degraded"), new ResourceLocation("shipwreck/sideways_fronthalf_degraded"), new ResourceLocation("shipwreck/sideways_backhalf_degraded"), new ResourceLocation("shipwreck/rightsideup_full_degraded"), new ResourceLocation("shipwreck/rightsideup_fronthalf_degraded"), new ResourceLocation("shipwreck/rightsideup_backhalf_degraded")};
    static final Map<String, ResourceLocation> MARKERS_TO_LOOT = Map.of("map_chest", BuiltInLootTables.SHIPWRECK_MAP, "treasure_chest", BuiltInLootTables.SHIPWRECK_TREASURE, "supply_chest", BuiltInLootTables.SHIPWRECK_SUPPLY);

    public static void addPieces(StructureTemplateManager structureTemplateManager, BlockPos pos, Rotation rotation, StructurePieceAccessor holder, RandomSource random, boolean bl) {
        ResourceLocation resourceLocation = Util.getRandom(bl ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN, random);
        holder.addPiece(new ShipwreckPieces.ShipwreckPiece(structureTemplateManager, resourceLocation, pos, rotation, bl));
    }

    public static class ShipwreckPiece extends TemplateStructurePiece {
        private final boolean isBeached;

        public ShipwreckPiece(StructureTemplateManager manager, ResourceLocation identifier, BlockPos pos, Rotation rotation, boolean grounded) {
            super(StructurePieceType.SHIPWRECK_PIECE, 0, manager, identifier, identifier.toString(), makeSettings(rotation), pos);
            this.isBeached = grounded;
        }

        public ShipwreckPiece(StructureTemplateManager manager, CompoundTag nbt) {
            super(StructurePieceType.SHIPWRECK_PIECE, nbt, manager, (resourceLocation) -> {
                return makeSettings(Rotation.valueOf(nbt.getString("Rot")));
            });
            this.isBeached = nbt.getBoolean("isBeached");
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag nbt) {
            super.addAdditionalSaveData(context, nbt);
            nbt.putBoolean("isBeached", this.isBeached);
            nbt.putString("Rot", this.placeSettings.getRotation().name());
        }

        private static StructurePlaceSettings makeSettings(Rotation rotation) {
            return (new StructurePlaceSettings()).setRotation(rotation).setMirror(Mirror.NONE).setRotationPivot(ShipwreckPieces.PIVOT).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
        }

        @Override
        protected void handleDataMarker(String metadata, BlockPos pos, ServerLevelAccessor world, RandomSource random, BoundingBox boundingBox) {
            ResourceLocation resourceLocation = ShipwreckPieces.MARKERS_TO_LOOT.get(metadata);
            if (resourceLocation != null) {
                RandomizableContainerBlockEntity.setLootTable(world, random, pos.below(), resourceLocation);
            }

        }

        @Override
        public void postProcess(WorldGenLevel world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
            int i = world.getMaxBuildHeight();
            int j = 0;
            Vec3i vec3i = this.template.getSize();
            Heightmap.Types types = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
            int k = vec3i.getX() * vec3i.getZ();
            if (k == 0) {
                j = world.getHeight(types, this.templatePosition.getX(), this.templatePosition.getZ());
            } else {
                BlockPos blockPos = this.templatePosition.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);

                for(BlockPos blockPos2 : BlockPos.betweenClosed(this.templatePosition, blockPos)) {
                    int l = world.getHeight(types, blockPos2.getX(), blockPos2.getZ());
                    j += l;
                    i = Math.min(i, l);
                }

                j /= k;
            }

            int m = this.isBeached ? i - vec3i.getY() / 2 - random.nextInt(3) : j;
            this.templatePosition = new BlockPos(this.templatePosition.getX(), m, this.templatePosition.getZ());
            super.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot);
        }
    }
}
