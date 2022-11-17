package net.minecraft.world.entity.ai.sensing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.frog.Frog;

public class FrogAttackablesSensor extends NearestVisibleLivingEntitySensor {
    public static final float TARGET_DETECTION_DISTANCE = 10.0F;

    @Override
    protected boolean isMatchingEntity(LivingEntity entity, LivingEntity target) {
        return !entity.getBrain().hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN) && Sensor.isEntityAttackable(entity, target) && Frog.canEat(target) && !this.isUnreachableAttackTarget(entity, target) ? target.closerThan(entity, 10.0D) : false;
    }

    private boolean isUnreachableAttackTarget(LivingEntity entity, LivingEntity target) {
        List<UUID> list = entity.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TONGUE_TARGETS).orElseGet(ArrayList::new);
        return list.contains(target.getUUID());
    }

    @Override
    protected MemoryModuleType<LivingEntity> getMemory() {
        return MemoryModuleType.NEAREST_ATTACKABLE;
    }
}
