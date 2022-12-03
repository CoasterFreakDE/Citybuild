package net.minecraft.world.level.gameevent;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EntityPositionSource implements PositionSource {
    public static final Codec<EntityPositionSource> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(ExtraCodecs.UUID.fieldOf("source_entity").forGetter(EntityPositionSource::getUuid), Codec.FLOAT.fieldOf("y_offset").orElse(0.0F).forGetter((entityPositionSource) -> {
            return entityPositionSource.yOffset;
        })).apply(instance, (uuid, yOffset) -> {
            return new EntityPositionSource(Either.right(Either.left(uuid)), yOffset);
        });
    });
    private Either<Entity, Either<UUID, Integer>> entityOrUuidOrId;
    final float yOffset;

    public EntityPositionSource(Entity entity, float yOffset) {
        this(Either.left(entity), yOffset);
    }

    EntityPositionSource(Either<Entity, Either<UUID, Integer>> source, float yOffset) {
        this.entityOrUuidOrId = source;
        this.yOffset = yOffset;
    }

    @Override
    public Optional<Vec3> getPosition(Level world) {
        if (this.entityOrUuidOrId.left().isEmpty()) {
            this.resolveEntity(world);
        }

        return this.entityOrUuidOrId.left().map((entity) -> {
            return entity.position().add(0.0D, (double)this.yOffset, 0.0D);
        });
    }

    private void resolveEntity(Level world) {
        this.entityOrUuidOrId.map(Optional::of, (entityId) -> {
            return Optional.ofNullable(entityId.map((uuid) -> {
                Entity var10000;
                if (world instanceof ServerLevel serverLevel) {
                    var10000 = serverLevel.getEntity(uuid);
                } else {
                    var10000 = null;
                }

                return var10000;
            }, world::getEntity));
        }).ifPresent((entity) -> {
            this.entityOrUuidOrId = Either.left(entity);
        });
    }

    private UUID getUuid() {
        return this.entityOrUuidOrId.map(Entity::getUUID, (entityId) -> {
            return entityId.map(Function.identity(), (entityIdx) -> {
                throw new RuntimeException("Unable to get entityId from uuid");
            });
        });
    }

    int getId() {
        return this.entityOrUuidOrId.map(Entity::getId, (entityId) -> {
            return entityId.map((uuid) -> {
                throw new IllegalStateException("Unable to get entityId from uuid");
            }, Function.identity());
        });
    }

    @Override
    public PositionSourceType<?> getType() {
        return PositionSourceType.ENTITY;
    }

    public static class Type implements PositionSourceType<EntityPositionSource> {
        @Override
        public EntityPositionSource read(FriendlyByteBuf friendlyByteBuf) {
            return new EntityPositionSource(Either.right(Either.right(friendlyByteBuf.readVarInt())), friendlyByteBuf.readFloat());
        }

        @Override
        public void write(FriendlyByteBuf buf, EntityPositionSource positionSource) {
            buf.writeVarInt(positionSource.getId());
            buf.writeFloat(positionSource.yOffset);
        }

        @Override
        public Codec<EntityPositionSource> codec() {
            return EntityPositionSource.CODEC;
        }
    }
}
