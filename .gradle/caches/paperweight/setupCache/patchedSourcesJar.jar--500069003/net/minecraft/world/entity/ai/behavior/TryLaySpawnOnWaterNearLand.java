package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;

public class TryLaySpawnOnWaterNearLand extends Behavior<Frog> {
    private final Block spawnBlock;
    private final MemoryModuleType<?> memoryModule;

    public TryLaySpawnOnWaterNearLand(Block frogSpawn, MemoryModuleType<?> triggerMemory) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.IS_PREGNANT, MemoryStatus.VALUE_PRESENT));
        this.spawnBlock = frogSpawn;
        this.memoryModule = triggerMemory;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, Frog entity) {
        return !entity.isInWater() && entity.isOnGround();
    }

    @Override
    protected void start(ServerLevel world, Frog entity, long time) {
        BlockPos blockPos = entity.blockPosition().below();

        for(Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockPos2 = blockPos.relative(direction);
            if (world.getBlockState(blockPos2).getCollisionShape(world, blockPos2).getFaceShape(Direction.UP).isEmpty() && world.getFluidState(blockPos2).is(Fluids.WATER)) {
                BlockPos blockPos3 = blockPos2.above();
                if (world.getBlockState(blockPos3).isAir()) {
                    // Paper start
                    if (org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory.callEntityChangeBlockEvent(entity, blockPos3, this.spawnBlock.defaultBlockState()).isCancelled()) {
                        entity.getBrain().eraseMemory(this.memoryModule); // forgot pregnant memory
                        return;
                    }
                    // Paper end
                    world.setBlock(blockPos3, this.spawnBlock.defaultBlockState(), 3);
                    world.playSound((Player)null, entity, SoundEvents.FROG_LAY_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                    entity.getBrain().eraseMemory(this.memoryModule);
                    return;
                }
            }
        }

    }
}
