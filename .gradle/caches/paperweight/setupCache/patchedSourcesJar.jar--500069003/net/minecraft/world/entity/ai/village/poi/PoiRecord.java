package net.minecraft.world.entity.ai.village.poi;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.VisibleForDebug;

public class PoiRecord {
    private final BlockPos pos;
    private final Holder<PoiType> poiType;
    private int freeTickets;
    private final Runnable setDirty;

    public static Codec<PoiRecord> codec(Runnable updateListener) {
        return RecordCodecBuilder.create((instance) -> {
            return instance.group(BlockPos.CODEC.fieldOf("pos").forGetter((poi) -> {
                return poi.pos;
            }), RegistryFixedCodec.create(Registry.POINT_OF_INTEREST_TYPE_REGISTRY).fieldOf("type").forGetter((poi) -> {
                return poi.poiType;
            }), Codec.INT.fieldOf("free_tickets").orElse(0).forGetter((poi) -> {
                return poi.freeTickets;
            }), RecordCodecBuilder.point(updateListener)).apply(instance, PoiRecord::new);
        });
    }

    private PoiRecord(BlockPos pos, Holder<PoiType> holder, int freeTickets, Runnable updateListener) {
        this.pos = pos.immutable();
        this.poiType = holder;
        this.freeTickets = freeTickets;
        this.setDirty = updateListener;
    }

    public PoiRecord(BlockPos pos, Holder<PoiType> holder, Runnable updateListener) {
        this(pos, holder, holder.value().maxTickets(), updateListener);
    }

    /** @deprecated */
    @Deprecated
    @VisibleForDebug
    public int getFreeTickets() {
        return this.freeTickets;
    }

    protected boolean acquireTicket() {
        if (this.freeTickets <= 0) {
            return false;
        } else {
            --this.freeTickets;
            this.setDirty.run();
            return true;
        }
    }

    protected boolean releaseTicket() {
        if (this.freeTickets >= this.poiType.value().maxTickets()) {
            return false;
        } else {
            ++this.freeTickets;
            this.setDirty.run();
            return true;
        }
    }

    public boolean hasSpace() {
        return this.freeTickets > 0;
    }

    public boolean isOccupied() {
        return this.freeTickets != this.poiType.value().maxTickets();
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Holder<PoiType> getPoiType() {
        return this.poiType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else {
            return object != null && this.getClass() == object.getClass() ? Objects.equals(this.pos, ((PoiRecord)object).pos) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.pos.hashCode();
    }
}
