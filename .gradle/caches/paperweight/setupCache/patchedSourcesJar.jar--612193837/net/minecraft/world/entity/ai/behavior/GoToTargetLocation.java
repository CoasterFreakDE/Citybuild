package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class GoToTargetLocation<E extends Mob> extends Behavior<E> {
    private final MemoryModuleType<BlockPos> locationMemory;
    private final int closeEnoughDist;
    private final float speedModifier;

    public GoToTargetLocation(MemoryModuleType<BlockPos> memoryModuleType, int completionRange, float speed) {
        super(ImmutableMap.of(memoryModuleType, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
        this.locationMemory = memoryModuleType;
        this.closeEnoughDist = completionRange;
        this.speedModifier = speed;
    }

    @Override
    protected void start(ServerLevel world, Mob entity, long time) {
        BlockPos blockPos = this.getTargetLocation(entity);
        boolean bl = blockPos.closerThan(entity.blockPosition(), (double)this.closeEnoughDist);
        if (!bl) {
            BehaviorUtils.setWalkAndLookTargetMemories(entity, getNearbyPos(entity, blockPos), this.speedModifier, this.closeEnoughDist);
        }

    }

    private static BlockPos getNearbyPos(Mob mob, BlockPos pos) {
        RandomSource randomSource = mob.level.random;
        return pos.offset(getRandomOffset(randomSource), 0, getRandomOffset(randomSource));
    }

    private static int getRandomOffset(RandomSource random) {
        return random.nextInt(3) - 1;
    }

    private BlockPos getTargetLocation(Mob entity) {
        return entity.getBrain().getMemory(this.locationMemory).get();
    }
}
