package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;

public class SetWardenLookTarget extends Behavior<Warden> {
    public SetWardenLookTarget() {
        super(ImmutableMap.of(MemoryModuleType.DISTURBANCE_LOCATION, MemoryStatus.REGISTERED, MemoryModuleType.ROAR_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Warden entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION) || entity.getBrain().hasMemoryValue(MemoryModuleType.ROAR_TARGET);
    }

    @Override
    protected void start(ServerLevel world, Warden entity, long time) {
        BlockPos blockPos = entity.getBrain().getMemory(MemoryModuleType.ROAR_TARGET).map(Entity::blockPosition).or(() -> {
            return entity.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION);
        }).get();
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(blockPos));
    }
}
