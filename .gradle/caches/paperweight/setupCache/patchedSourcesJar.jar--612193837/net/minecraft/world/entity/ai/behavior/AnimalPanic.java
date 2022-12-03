package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;

public class AnimalPanic extends Behavior<PathfinderMob> {
    private static final int PANIC_MIN_DURATION = 100;
    private static final int PANIC_MAX_DURATION = 120;
    private static final int PANIC_DISTANCE_HORIZONTAL = 5;
    private static final int PANIC_DISTANCE_VERTICAL = 4;
    private final float speedMultiplier;

    public AnimalPanic(float speed) {
        super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.REGISTERED, MemoryModuleType.HURT_BY, MemoryStatus.VALUE_PRESENT), 100, 120);
        this.speedMultiplier = speed;
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
        return true;
    }

    @Override
    protected void start(ServerLevel serverLevel, PathfinderMob pathfinderMob, long l) {
        pathfinderMob.getBrain().setMemory(MemoryModuleType.IS_PANICKING, true);
        pathfinderMob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void stop(ServerLevel world, PathfinderMob entity, long time) {
        Brain<?> brain = entity.getBrain();
        brain.eraseMemory(MemoryModuleType.IS_PANICKING);
    }

    @Override
    protected void tick(ServerLevel world, PathfinderMob entity, long time) {
        if (entity.getNavigation().isDone()) {
            Vec3 vec3 = this.getPanicPos(entity, world);
            if (vec3 != null) {
                entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, this.speedMultiplier, 0));
            }
        }

    }

    @Nullable
    private Vec3 getPanicPos(PathfinderMob entity, ServerLevel world) {
        if (entity.isOnFire()) {
            Optional<Vec3> optional = this.lookForWater(world, entity).map(Vec3::atBottomCenterOf);
            if (optional.isPresent()) {
                return optional.get();
            }
        }

        return LandRandomPos.getPos(entity, 5, 4);
    }

    private Optional<BlockPos> lookForWater(BlockGetter world, Entity entity) {
        BlockPos blockPos = entity.blockPosition();
        return !world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty() ? Optional.empty() : BlockPos.findClosestMatch(blockPos, 5, 1, (pos) -> {
            return world.getFluidState(pos).is(FluidTags.WATER);
        });
    }
}
