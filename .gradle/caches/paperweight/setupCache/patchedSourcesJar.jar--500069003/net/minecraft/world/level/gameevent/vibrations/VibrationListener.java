package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VibrationListener implements GameEventListener {
    protected final PositionSource listenerSource;
    public int listenerRange;
    protected final VibrationListener.VibrationListenerConfig config;
    @Nullable
    protected VibrationListener.ReceivingEvent receivingEvent;
    protected float receivingDistance;
    protected int travelTimeInTicks;

    public static Codec<VibrationListener> codec(VibrationListener.VibrationListenerConfig callback) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(PositionSource.CODEC.fieldOf("source").forGetter((listener) -> {
                return listener.listenerSource;
            }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("range").forGetter((listener) -> {
                return listener.listenerRange;
            }), VibrationListener.ReceivingEvent.CODEC.optionalFieldOf("event").forGetter((listener) -> {
                return Optional.ofNullable(listener.receivingEvent);
            }), Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("event_distance").orElse(0.0F).forGetter((listener) -> {
                return listener.receivingDistance;
            }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("event_delay").orElse(0).forGetter((listener) -> {
                return listener.travelTimeInTicks;
            })).apply(instance, (positionSource, range, vibration, distance, delay) -> {
                return new VibrationListener(positionSource, range, callback, vibration.orElse((VibrationListener.ReceivingEvent)null), distance, delay);
            });
        });
    }

    public VibrationListener(PositionSource positionSource, int range, VibrationListener.VibrationListenerConfig callback, @Nullable VibrationListener.ReceivingEvent vibration, float distance, int delay) {
        this.listenerSource = positionSource;
        this.listenerRange = range;
        this.config = callback;
        this.receivingEvent = vibration;
        this.receivingDistance = distance;
        this.travelTimeInTicks = delay;
    }

    public void tick(Level world) {
        if (world instanceof ServerLevel serverLevel) {
            if (this.receivingEvent != null) {
                --this.travelTimeInTicks;
                if (this.travelTimeInTicks <= 0) {
                    this.travelTimeInTicks = 0;
                    this.config.onSignalReceive(serverLevel, this, new BlockPos(this.receivingEvent.pos), this.receivingEvent.gameEvent, this.receivingEvent.getEntity(serverLevel).orElse((Entity)null), this.receivingEvent.getProjectileOwner(serverLevel).orElse((Entity)null), this.receivingDistance);
                    this.receivingEvent = null;
                }
            }
        }

    }

    @Override
    public PositionSource getListenerSource() {
        return this.listenerSource;
    }

    @Override
    public int getListenerRadius() {
        return this.listenerRange;
    }

    @Override
    public boolean handleGameEvent(ServerLevel world, GameEvent.Message event) {
        if (this.receivingEvent != null) {
            return false;
        } else {
            GameEvent gameEvent = event.gameEvent();
            GameEvent.Context context = event.context();
            if (!this.config.isValidVibration(gameEvent, context)) {
                return false;
            } else {
                Optional<Vec3> optional = this.listenerSource.getPosition(world);
                if (optional.isEmpty()) {
                    return false;
                } else {
                    Vec3 vec3 = event.source();
                    Vec3 vec32 = optional.get();
                    if (!this.config.shouldListen(world, this, new BlockPos(vec3), gameEvent, context)) {
                        return false;
                    } else if (isOccluded(world, vec3, vec32)) {
                        return false;
                    } else {
                        this.scheduleSignal(world, gameEvent, context, vec3, vec32);
                        return true;
                    }
                }
            }
        }
    }

    private void scheduleSignal(ServerLevel world, GameEvent gameEvent, GameEvent.Context emitter, Vec3 start, Vec3 end) {
        this.receivingDistance = (float)start.distanceTo(end);
        this.receivingEvent = new VibrationListener.ReceivingEvent(gameEvent, this.receivingDistance, start, emitter.sourceEntity());
        this.travelTimeInTicks = Mth.floor(this.receivingDistance);
        world.sendParticles(new VibrationParticleOption(this.listenerSource, this.travelTimeInTicks), start.x, start.y, start.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        this.config.onSignalSchedule();
    }

    private static boolean isOccluded(Level world, Vec3 start, Vec3 end) {
        Vec3 vec3 = new Vec3((double)Mth.floor(start.x) + 0.5D, (double)Mth.floor(start.y) + 0.5D, (double)Mth.floor(start.z) + 0.5D);
        Vec3 vec32 = new Vec3((double)Mth.floor(end.x) + 0.5D, (double)Mth.floor(end.y) + 0.5D, (double)Mth.floor(end.z) + 0.5D);

        for(Direction direction : Direction.values()) {
            Vec3 vec33 = vec3.relative(direction, (double)1.0E-5F);
            if (world.isBlockInLine(new ClipBlockStateContext(vec33, vec32, (state) -> {
                return state.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS);
            })).getType() != HitResult.Type.BLOCK) {
                return false;
            }
        }

        return true;
    }

    public static record ReceivingEvent(GameEvent gameEvent, float distance, Vec3 pos, @Nullable UUID uuid, @Nullable UUID projectileOwnerUuid, @Nullable Entity entity) {
        public static final Codec<VibrationListener.ReceivingEvent> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Registry.GAME_EVENT.byNameCodec().fieldOf("game_event").forGetter(VibrationListener.ReceivingEvent::gameEvent), Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("distance").forGetter(VibrationListener.ReceivingEvent::distance), Vec3.CODEC.fieldOf("pos").forGetter(VibrationListener.ReceivingEvent::pos), ExtraCodecs.UUID.optionalFieldOf("source").forGetter((vibration) -> {
                return Optional.ofNullable(vibration.uuid());
            }), ExtraCodecs.UUID.optionalFieldOf("projectile_owner").forGetter((vibration) -> {
                return Optional.ofNullable(vibration.projectileOwnerUuid());
            })).apply(instance, (event, distance, pos, uuid, projectileOwnerUuid) -> {
                return new VibrationListener.ReceivingEvent(event, distance, pos, uuid.orElse((UUID)null), projectileOwnerUuid.orElse((UUID)null));
            });
        });

        public ReceivingEvent(GameEvent gameEvent, float distance, Vec3 pos, @Nullable UUID uuid, @Nullable UUID projectileOwnerUuid) {
            this(gameEvent, distance, pos, uuid, projectileOwnerUuid, (Entity)null);
        }

        public ReceivingEvent(GameEvent gameEvent, float distance, Vec3 pos, @Nullable Entity entity) {
            this(gameEvent, distance, pos, entity == null ? null : entity.getUUID(), getProjectileOwner(entity), entity);
        }

        @Nullable
        private static UUID getProjectileOwner(@Nullable Entity entity) {
            if (entity instanceof Projectile projectile) {
                if (projectile.getOwner() != null) {
                    return projectile.getOwner().getUUID();
                }
            }

            return null;
        }

        public Optional<Entity> getEntity(ServerLevel world) {
            return Optional.ofNullable(this.entity).or(() -> {
                return Optional.ofNullable(this.uuid).map(world::getEntity);
            });
        }

        public Optional<Entity> getProjectileOwner(ServerLevel world) {
            return this.getEntity(world).filter((entity) -> {
                return entity instanceof Projectile;
            }).map((entity) -> {
                return (Projectile)entity;
            }).map(Projectile::getOwner).or(() -> {
                return Optional.ofNullable(this.projectileOwnerUuid).map(world::getEntity);
            });
        }
    }

    public interface VibrationListenerConfig {
        default TagKey<GameEvent> getListenableEvents() {
            return GameEventTags.VIBRATIONS;
        }

        default boolean canTriggerAvoidVibration() {
            return false;
        }

        default boolean isValidVibration(GameEvent gameEvent, GameEvent.Context emitter) {
            if (!gameEvent.is(this.getListenableEvents())) {
                return false;
            } else {
                Entity entity = emitter.sourceEntity();
                if (entity != null) {
                    if (entity.isSpectator()) {
                        return false;
                    }

                    if (entity.isSteppingCarefully() && gameEvent.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
                        if (this.canTriggerAvoidVibration() && entity instanceof ServerPlayer) {
                            ServerPlayer serverPlayer = (ServerPlayer)entity;
                            CriteriaTriggers.AVOID_VIBRATION.trigger(serverPlayer);
                        }

                        return false;
                    }

                    if (entity.dampensVibrations()) {
                        return false;
                    }
                }

                if (emitter.affectedState() != null) {
                    return !emitter.affectedState().is(BlockTags.DAMPENS_VIBRATIONS);
                } else {
                    return true;
                }
            }
        }

        boolean shouldListen(ServerLevel world, GameEventListener listener, BlockPos pos, GameEvent event, GameEvent.Context emitter);

        void onSignalReceive(ServerLevel world, GameEventListener listener, BlockPos pos, GameEvent event, @Nullable Entity entity, @Nullable Entity sourceEntity, float distance);

        default void onSignalSchedule() {
        }
    }
}
