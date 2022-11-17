package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.StringUtil;

public record ServerboundChatPreviewPacket(int queryId, String query) implements Packet<ServerGamePacketListener> {
    public ServerboundChatPreviewPacket {
        string = StringUtil.trimChatMessage(string);
    }

    public ServerboundChatPreviewPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readUtf(256));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.queryId);
        buf.writeUtf(this.query, 256);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleChatPreview(this);
    }
}
