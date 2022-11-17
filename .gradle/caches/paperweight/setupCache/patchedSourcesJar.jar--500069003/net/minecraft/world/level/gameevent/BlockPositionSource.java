package net.minecraft.world.level.gameevent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BlockPositionSource implements PositionSource {
    public static final Codec<BlockPositionSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BlockPos.CODEC.fieldOf("pos").forGetter((blockPositionSource) -> {
            return blockPositionSource.pos;
        })).apply(instance, BlockPositionSource::new);
    });
    final BlockPos pos;

    public BlockPositionSource(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public Optional<Vec3> getPosition(Level world) {
        return Optional.of(Vec3.atCenterOf(this.pos));
    }

    @Override
    public PositionSourceType<?> getType() {
        return PositionSourceType.BLOCK;
    }

    public static class Type implements PositionSourceType<BlockPositionSource> {
        @Override
        public BlockPositionSource read(FriendlyByteBuf friendlyByteBuf) {
            return new BlockPositionSource(friendlyByteBuf.readBlockPos());
        }

        @Override
        public void write(FriendlyByteBuf buf, BlockPositionSource positionSource) {
            buf.writeBlockPos(positionSource.pos);
        }

        @Override
        public Codec<BlockPositionSource> codec() {
            return BlockPositionSource.CODEC;
        }
    }
}
