package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BeehiveDecorator extends TreeDecorator {
    public static final Codec<BeehiveDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(BeehiveDecorator::new, (decorator) -> {
        return decorator.probability;
    }).codec();
    private static final Direction WORLDGEN_FACING = Direction.SOUTH;
    private static final Direction[] SPAWN_DIRECTIONS = Direction.Plane.HORIZONTAL.stream().filter((direction) -> {
        return direction != WORLDGEN_FACING.getOpposite();
    }).toArray((i) -> {
        return new Direction[i];
    });
    private final float probability;

    public BeehiveDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.BEEHIVE;
    }

    @Override
    public void place(TreeDecorator.Context generator) {
        RandomSource randomSource = generator.random();
        if (!(randomSource.nextFloat() >= this.probability)) {
            List<BlockPos> list = generator.leaves();
            List<BlockPos> list2 = generator.logs();
            int i = !list.isEmpty() ? Math.max(list.get(0).getY() - 1, list2.get(0).getY() + 1) : Math.min(list2.get(0).getY() + 1 + randomSource.nextInt(3), list2.get(list2.size() - 1).getY());
            List<BlockPos> list3 = list2.stream().filter((pos) -> {
                return pos.getY() == i;
            }).flatMap((pos) -> {
                return Stream.of(SPAWN_DIRECTIONS).map(pos::relative);
            }).collect(Collectors.toList());
            if (!list3.isEmpty()) {
                Collections.shuffle(list3);
                Optional<BlockPos> optional = list3.stream().filter((pos) -> {
                    return generator.isAir(pos) && generator.isAir(pos.relative(WORLDGEN_FACING));
                }).findFirst();
                if (!optional.isEmpty()) {
                    generator.setBlock(optional.get(), Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, WORLDGEN_FACING));
                    generator.level().getBlockEntity(optional.get(), BlockEntityType.BEEHIVE).ifPresent((blockEntity) -> {
                        int i = 2 + randomSource.nextInt(2);

                        for(int j = 0; j < i; ++j) {
                            CompoundTag compoundTag = new CompoundTag();
                            compoundTag.putString("id", Registry.ENTITY_TYPE.getKey(EntityType.BEE).toString());
                            blockEntity.storeBee(compoundTag, randomSource.nextInt(599), false);
                        }

                    });
                }
            }
        }
    }
}
