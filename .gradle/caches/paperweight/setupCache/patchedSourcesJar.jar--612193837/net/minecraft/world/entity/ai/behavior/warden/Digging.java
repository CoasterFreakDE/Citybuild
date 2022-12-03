package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;

public class Digging<E extends Warden> extends Behavior<E> {
    public Digging(int duration) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), duration);
    }

    @Override
    protected boolean canStillUse(ServerLevel serverLevel, E warden, long l) {
        return warden.getRemovalReason() == null;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, E entity) {
        return entity.isOnGround() || entity.isInWater() || entity.isInLava();
    }

    @Override
    protected void start(ServerLevel serverLevel, E warden, long l) {
        if (warden.isOnGround()) {
            warden.setPose(Pose.DIGGING);
            warden.playSound(SoundEvents.WARDEN_DIG, 5.0F, 1.0F);
        } else {
            warden.playSound(SoundEvents.WARDEN_AGITATED, 5.0F, 1.0F);
            this.stop(serverLevel, warden, l);
        }

    }

    @Override
    protected void stop(ServerLevel world, E entity, long time) {
        if (entity.getRemovalReason() == null) {
            entity.remove(Entity.RemovalReason.DISCARDED);
        }

    }
}
