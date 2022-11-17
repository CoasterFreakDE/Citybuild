package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class ClientboundMapItemDataPacket implements Packet<ClientGamePacketListener> {
    private final int mapId;
    private final byte scale;
    private final boolean locked;
    @Nullable
    private final List<MapDecoration> decorations;
    @Nullable
    private final MapItemSavedData.MapPatch colorPatch;

    public ClientboundMapItemDataPacket(int id, byte scale, boolean showIcons, @Nullable Collection<MapDecoration> icons, @Nullable MapItemSavedData.MapPatch updateData) {
        this.mapId = id;
        this.scale = scale;
        this.locked = showIcons;
        this.decorations = icons != null ? Lists.newArrayList(icons) : null;
        this.colorPatch = updateData;
    }

    public ClientboundMapItemDataPacket(FriendlyByteBuf buf) {
        this.mapId = buf.readVarInt();
        this.scale = buf.readByte();
        this.locked = buf.readBoolean();
        this.decorations = buf.readNullable((buf2) -> {
            return buf2.readList((buf3) -> {
                MapDecoration.Type type = buf3.readEnum(MapDecoration.Type.class);
                byte b = buf3.readByte();
                byte c = buf3.readByte();
                byte d = (byte)(buf3.readByte() & 15);
                Component component = buf3.readNullable(FriendlyByteBuf::readComponent);
                return new MapDecoration(type, b, c, d, component);
            });
        });
        int i = buf.readUnsignedByte();
        if (i > 0) {
            int j = buf.readUnsignedByte();
            int k = buf.readUnsignedByte();
            int l = buf.readUnsignedByte();
            byte[] bs = buf.readByteArray();
            this.colorPatch = new MapItemSavedData.MapPatch(k, l, i, j, bs);
        } else {
            this.colorPatch = null;
        }

    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.mapId);
        buf.writeByte(this.scale);
        buf.writeBoolean(this.locked);
        buf.writeNullable(this.decorations, (buf2, icons) -> {
            buf2.writeCollection(icons, (b, icon) -> {
                b.writeEnum(icon.getType());
                b.writeByte(icon.getX());
                b.writeByte(icon.getY());
                b.writeByte(icon.getRot() & 15);
                b.writeNullable(icon.getName(), FriendlyByteBuf::writeComponent);
            });
        });
        if (this.colorPatch != null) {
            buf.writeByte(this.colorPatch.width);
            buf.writeByte(this.colorPatch.height);
            buf.writeByte(this.colorPatch.startX);
            buf.writeByte(this.colorPatch.startY);
            buf.writeByteArray(this.colorPatch.mapColors);
        } else {
            buf.writeByte(0);
        }

    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleMapItemData(this);
    }

    public int getMapId() {
        return this.mapId;
    }

    public void applyToMap(MapItemSavedData mapState) {
        if (this.decorations != null) {
            mapState.addClientSideDecorations(this.decorations);
        }

        if (this.colorPatch != null) {
            this.colorPatch.applyToMap(mapState);
        }

    }

    public byte getScale() {
        return this.scale;
    }

    public boolean isLocked() {
        return this.locked;
    }
}
