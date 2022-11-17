package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;

public class SculkCatalystBlockEntity extends BlockEntity implements GameEventListener {
    private final BlockPositionSource blockPosSource = new BlockPositionSource(this.worldPosition);
    private final SculkSpreader sculkSpreader = SculkSpreader.createLevelSpreader();

    public SculkCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.SCULK_CATALYST, pos, state);
    }

    @Override
    public boolean handleEventsImmediately() {
        return true;
    }

    @Override
    public PositionSource getListenerSource() {
        return this.blockPosSource;
    }

    @Override
    public int getListenerRadius() {
        return 8;
    }

    @Override
    public boolean handleGameEvent(ServerLevel world, GameEvent.Message event) {
        if (this.isRemoved()) {
            return false;
        } else {
            GameEvent.Context context = event.context();
            if (event.gameEvent() == GameEvent.ENTITY_DIE) {
                Entity i = context.sourceEntity();
                if (i instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity)i;
                    if (!livingEntity.wasExperienceConsumed()) {
                        int i = livingEntity.getExperienceReward();
                        if (livingEntity.shouldDropExperience() && i > 0) {
                            this.sculkSpreader.addCursors(new BlockPos(event.source().relative(Direction.UP, 0.5D)), i);
                            LivingEntity livingEntity2 = livingEntity.getLastHurtByMob();
                            if (livingEntity2 instanceof ServerPlayer) {
                                ServerPlayer serverPlayer = (ServerPlayer)livingEntity2;
                                DamageSource damageSource = livingEntity.getLastDamageSource() == null ? DamageSource.playerAttack(serverPlayer) : livingEntity.getLastDamageSource();
                                CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST.trigger(serverPlayer, context.sourceEntity(), damageSource);
                            }
                        }

                        livingEntity.skipDropExperience();
                        SculkCatalystBlock.bloom(world, this.worldPosition, this.getBlockState(), world.getRandom());
                    }

                    return true;
                }
            }

            return false;
        }
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, SculkCatalystBlockEntity blockEntity) {
        blockEntity.sculkSpreader.updateCursors(world, pos, world.getRandom(), true);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.sculkSpreader.load(nbt);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        this.sculkSpreader.save(nbt);
        super.saveAdditional(nbt);
    }

    @VisibleForTesting
    public SculkSpreader getSculkSpreader() {
        return this.sculkSpreader;
    }
}
