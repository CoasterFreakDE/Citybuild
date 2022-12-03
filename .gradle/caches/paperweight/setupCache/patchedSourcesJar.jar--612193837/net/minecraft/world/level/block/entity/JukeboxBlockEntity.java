package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class JukeboxBlockEntity extends BlockEntity implements Clearable {
    private ItemStack record = ItemStack.EMPTY;
    private int ticksSinceLastEvent;
    private long tickCount;
    private long recordStartedTick;
    private boolean isPlaying;

    public JukeboxBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityType.JUKEBOX, pos, state);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("RecordItem", 10)) {
            this.setRecord(ItemStack.of(nbt.getCompound("RecordItem")));
        }

        this.isPlaying = nbt.getBoolean("IsPlaying");
        this.recordStartedTick = nbt.getLong("RecordStartTick");
        this.tickCount = nbt.getLong("TickCount");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (!this.getRecord().isEmpty()) {
            nbt.put("RecordItem", this.getRecord().save(new CompoundTag()));
        }

        nbt.putBoolean("IsPlaying", this.isPlaying);
        nbt.putLong("RecordStartTick", this.recordStartedTick);
        nbt.putLong("TickCount", this.tickCount);
    }

    public ItemStack getRecord() {
        return this.record;
    }

    public void setRecord(ItemStack stack) {
        this.record = stack;
        this.setChanged();
    }

    public void playRecord() {
        this.recordStartedTick = this.tickCount;
        this.isPlaying = true;
    }

    @Override
    public void clearContent() {
        this.setRecord(ItemStack.EMPTY);
        this.isPlaying = false;
    }

    public static void playRecordTick(Level world, BlockPos pos, BlockState state, JukeboxBlockEntity blockEntity) {
        ++blockEntity.ticksSinceLastEvent;
        if (recordIsPlaying(state, blockEntity)) {
            Item var5 = blockEntity.getRecord().getItem();
            if (var5 instanceof RecordItem) {
                RecordItem recordItem = (RecordItem)var5;
                if (recordShouldStopPlaying(blockEntity, recordItem)) {
                    world.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, pos, GameEvent.Context.of(state));
                    blockEntity.isPlaying = false;
                } else if (shouldSendJukeboxPlayingEvent(blockEntity)) {
                    blockEntity.ticksSinceLastEvent = 0;
                    world.gameEvent(GameEvent.JUKEBOX_PLAY, pos, GameEvent.Context.of(state));
                }
            }
        }

        ++blockEntity.tickCount;
    }

    private static boolean recordIsPlaying(BlockState state, JukeboxBlockEntity blockEntity) {
        return state.getValue(JukeboxBlock.HAS_RECORD) && blockEntity.isPlaying;
    }

    private static boolean recordShouldStopPlaying(JukeboxBlockEntity blockEntity, RecordItem musicDisc) {
        return blockEntity.tickCount >= blockEntity.recordStartedTick + (long)musicDisc.getLengthInTicks();
    }

    private static boolean shouldSendJukeboxPlayingEvent(JukeboxBlockEntity blockEntity) {
        return blockEntity.ticksSinceLastEvent >= 20;
    }
}
