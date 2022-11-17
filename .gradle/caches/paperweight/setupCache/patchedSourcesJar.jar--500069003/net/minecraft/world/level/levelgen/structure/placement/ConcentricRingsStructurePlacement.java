package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public class ConcentricRingsStructurePlacement extends StructurePlacement {
    public static final Codec<ConcentricRingsStructurePlacement> CODEC = RecordCodecBuilder.create((instance) -> {
        return codec(instance).apply(instance, ConcentricRingsStructurePlacement::new);
    });
    private final int distance;
    private final int spread;
    private final int count;
    private final HolderSet<Biome> preferredBiomes;

    private static Products.P9<RecordCodecBuilder.Mu<ConcentricRingsStructurePlacement>, Vec3i, StructurePlacement.FrequencyReductionMethod, Float, Integer, Optional<StructurePlacement.ExclusionZone>, Integer, Integer, Integer, HolderSet<Biome>> codec(RecordCodecBuilder.Instance<ConcentricRingsStructurePlacement> instance) {
        Products.P5<RecordCodecBuilder.Mu<ConcentricRingsStructurePlacement>, Vec3i, StructurePlacement.FrequencyReductionMethod, Float, Integer, Optional<StructurePlacement.ExclusionZone>> p5 = placementCodec(instance);
        Products.P4<RecordCodecBuilder.Mu<ConcentricRingsStructurePlacement>, Integer, Integer, Integer, HolderSet<Biome>> p4 = instance.group(Codec.intRange(0, 1023).fieldOf("distance").forGetter(ConcentricRingsStructurePlacement::distance), Codec.intRange(0, 1023).fieldOf("spread").forGetter(ConcentricRingsStructurePlacement::spread), Codec.intRange(1, 4095).fieldOf("count").forGetter(ConcentricRingsStructurePlacement::count), RegistryCodecs.homogeneousList(Registry.BIOME_REGISTRY).fieldOf("preferred_biomes").forGetter(ConcentricRingsStructurePlacement::preferredBiomes));
        return new Products.P9<>(p5.t1(), p5.t2(), p5.t3(), p5.t4(), p5.t5(), p4.t1(), p4.t2(), p4.t3(), p4.t4());
    }

    public ConcentricRingsStructurePlacement(Vec3i locateOffset, StructurePlacement.FrequencyReductionMethod generationPredicateType, float frequency, int salt, Optional<StructurePlacement.ExclusionZone> exclusionZone, int distance, int spread, int structureCount, HolderSet<Biome> preferredBiomes) {
        super(locateOffset, generationPredicateType, frequency, salt, exclusionZone);
        this.distance = distance;
        this.spread = spread;
        this.count = structureCount;
        this.preferredBiomes = preferredBiomes;
    }

    public ConcentricRingsStructurePlacement(int distance, int spread, int structureCount, HolderSet<Biome> preferredBiomes) {
        this(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty(), distance, spread, structureCount, preferredBiomes);
    }

    public int distance() {
        return this.distance;
    }

    public int spread() {
        return this.spread;
    }

    public int count() {
        return this.count;
    }

    public HolderSet<Biome> preferredBiomes() {
        return this.preferredBiomes;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGenerator chunkGenerator, RandomState noiseConfig, long seed, int chunkX, int chunkZ) {
        List<ChunkPos> list = chunkGenerator.getRingPositionsFor(this, noiseConfig);
        return list == null ? false : list.contains(new ChunkPos(chunkX, chunkZ));
    }

    @Override
    public StructurePlacementType<?> type() {
        return StructurePlacementType.CONCENTRIC_RINGS;
    }
}
