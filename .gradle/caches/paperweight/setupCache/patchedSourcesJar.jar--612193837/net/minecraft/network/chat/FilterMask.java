package net.minecraft.network.chat;

import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;

public class FilterMask {
    public static final FilterMask FULLY_FILTERED = new FilterMask(new BitSet(0), FilterMask.Type.FULLY_FILTERED);
    public static final FilterMask PASS_THROUGH = new FilterMask(new BitSet(0), FilterMask.Type.PASS_THROUGH);
    private static final char HASH = '#';
    private final BitSet mask;
    private final FilterMask.Type type;

    private FilterMask(BitSet mask, FilterMask.Type status) {
        this.mask = mask;
        this.type = status;
    }

    public FilterMask(int length) {
        this(new BitSet(length), FilterMask.Type.PARTIALLY_FILTERED);
    }

    public static FilterMask read(FriendlyByteBuf buf) {
        FilterMask.Type type = buf.readEnum(FilterMask.Type.class);
        FilterMask var10000;
        switch (type) {
            case PASS_THROUGH:
                var10000 = PASS_THROUGH;
                break;
            case FULLY_FILTERED:
                var10000 = FULLY_FILTERED;
                break;
            case PARTIALLY_FILTERED:
                var10000 = new FilterMask(buf.readBitSet(), FilterMask.Type.PARTIALLY_FILTERED);
                break;
            default:
                throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    public static void write(FriendlyByteBuf buf, FilterMask mask) {
        buf.writeEnum(mask.type);
        if (mask.type == FilterMask.Type.PARTIALLY_FILTERED) {
            buf.writeBitSet(mask.mask);
        }

    }

    public void setFiltered(int index) {
        this.mask.set(index);
    }

    @Nullable
    public String apply(String raw) {
        String var10000;
        switch (this.type) {
            case PASS_THROUGH:
                var10000 = raw;
                break;
            case FULLY_FILTERED:
                var10000 = null;
                break;
            case PARTIALLY_FILTERED:
                char[] cs = raw.toCharArray();

                for(int i = 0; i < cs.length && i < this.mask.length(); ++i) {
                    if (this.mask.get(i)) {
                        cs[i] = '#';
                    }
                }

                var10000 = new String(cs);
                break;
            default:
                throw new IncompatibleClassChangeError();
        }

        return var10000;
    }

    @Nullable
    public Component apply(ChatMessageContent contents) {
        String string = contents.plain();
        return Util.mapNullable(this.apply(string), Component::literal);
    }

    public boolean isEmpty() {
        return this.type == FilterMask.Type.PASS_THROUGH;
    }

    public boolean isFullyFiltered() {
        return this.type == FilterMask.Type.FULLY_FILTERED;
    }

    static enum Type {
        PASS_THROUGH,
        FULLY_FILTERED,
        PARTIALLY_FILTERED;
    }
}
