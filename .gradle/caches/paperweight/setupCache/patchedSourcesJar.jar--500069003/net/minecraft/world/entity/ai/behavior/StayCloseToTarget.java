package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class StayCloseToTarget<E extends LivingEntity> extends Behavior<E> {
    private final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter;
    private final int closeEnough;
    private final int tooFar;
    private final float speedModifier;

    public StayCloseToTarget(Function<LivingEntity, Optional<PositionTracker>> lookTargetFunction, int completionRange, int searchRange, float speed) {
        super(Map.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.targetPositionGetter = lookTargetFunction;
        this.closeEnough = completionRange;
        this.tooFar = searchRange;
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        Optional<PositionTracker> optional = this.targetPositionGetter.apply(entity);
        if (optional.isEmpty()) {
            return false;
        } else {
            PositionTracker positionTracker = optional.get();
            return !entity.position().closerThan(positionTracker.currentPosition(), (double)this.tooFar);
        }
    }

    @Override
    protected void start(ServerLevel world, E entity, long time) {
        BehaviorUtils.setWalkAndLookTargetMemories(entity, this.targetPositionGetter.apply(entity).get(), this.speedModifier, this.closeEnough);
    }
}
