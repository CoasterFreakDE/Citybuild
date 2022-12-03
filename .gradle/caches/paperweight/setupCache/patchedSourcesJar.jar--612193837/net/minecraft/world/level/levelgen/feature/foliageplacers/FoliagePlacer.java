package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.material.Fluids;

public abstract class FoliagePlacer {
    public static final Codec<FoliagePlacer> CODEC = Registry.FOLIAGE_PLACER_TYPES.byNameCodec().dispatch(FoliagePlacer::type, FoliagePlacerType::codec);
    protected final IntProvider radius;
    protected final IntProvider offset;

    protected static <P extends FoliagePlacer> Products.P2<RecordCodecBuilder.Mu<P>, IntProvider, IntProvider> foliagePlacerParts(RecordCodecBuilder.Instance<P> instance) {
        return instance.group(IntProvider.codec(0, 16).fieldOf("radius").forGetter((placer) -> {
            return placer.radius;
        }), IntProvider.codec(0, 16).fieldOf("offset").forGetter((placer) -> {
            return placer.offset;
        }));
    }

    public FoliagePlacer(IntProvider radius, IntProvider offset) {
        this.radius = radius;
        this.offset = offset;
    }

    protected abstract FoliagePlacerType<?> type();

    public void createFoliage(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, RandomSource random, TreeConfiguration config, int trunkHeight, FoliagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius) {
        this.createFoliage(world, replacer, random, config, trunkHeight, treeNode, foliageHeight, radius, this.offset(random));
    }

    protected abstract void createFoliage(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, RandomSource random, TreeConfiguration config, int trunkHeight, FoliagePlacer.FoliageAttachment treeNode, int foliageHeight, int radius, int offset);

    public abstract int foliageHeight(RandomSource random, int trunkHeight, TreeConfiguration config);

    public int foliageRadius(RandomSource random, int baseHeight) {
        return this.radius.sample(random);
    }

    private int offset(RandomSource random) {
        return this.offset.sample(random);
    }

    protected abstract boolean shouldSkipLocation(RandomSource random, int dx, int y, int dz, int radius, boolean giantTrunk);

    protected boolean shouldSkipLocationSigned(RandomSource random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        int i;
        int j;
        if (giantTrunk) {
            i = Math.min(Math.abs(dx), Math.abs(dx - 1));
            j = Math.min(Math.abs(dz), Math.abs(dz - 1));
        } else {
            i = Math.abs(dx);
            j = Math.abs(dz);
        }

        return this.shouldSkipLocation(random, i, y, j, radius, giantTrunk);
    }

    protected void placeLeavesRow(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, RandomSource random, TreeConfiguration config, BlockPos centerPos, int radius, int y, boolean giantTrunk) {
        int i = giantTrunk ? 1 : 0;
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for(int j = -radius; j <= radius + i; ++j) {
            for(int k = -radius; k <= radius + i; ++k) {
                if (!this.shouldSkipLocationSigned(random, j, y, k, radius, giantTrunk)) {
                    mutableBlockPos.setWithOffset(centerPos, j, y, k);
                    tryPlaceLeaf(world, replacer, random, config, mutableBlockPos);
                }
            }
        }

    }

    protected static void tryPlaceLeaf(LevelSimulatedReader world, BiConsumer<BlockPos, BlockState> replacer, RandomSource random, TreeConfiguration config, BlockPos pos) {
        if (TreeFeature.validTreePos(world, pos)) {
            BlockState blockState = config.foliageProvider.getState(random, pos);
            if (blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                blockState = blockState.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(world.isFluidAtPosition(pos, (fluidState) -> {
                    return fluidState.isSourceOfType(Fluids.WATER);
                })));
            }

            replacer.accept(pos, blockState);
        }

    }

    public static final class FoliageAttachment {
        private final BlockPos pos;
        private final int radiusOffset;
        private final boolean doubleTrunk;

        public FoliageAttachment(BlockPos center, int foliageRadius, boolean giantTrunk) {
            this.pos = center;
            this.radiusOffset = foliageRadius;
            this.doubleTrunk = giantTrunk;
        }

        public BlockPos pos() {
            return this.pos;
        }

        public int radiusOffset() {
            return this.radiusOffset;
        }

        public boolean doubleTrunk() {
            return this.doubleTrunk;
        }
    }
}
