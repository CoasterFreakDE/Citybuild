package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;

public record ClientboundChatPreviewPacket(int queryId, @Nullable Component preview) implements Packet<ClientGamePacketListener> {
    public ClientboundChatPreviewPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readNullable(FriendlyByteBuf::readComponent));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(this.queryId);
        buf.writeNullable(this.preview, FriendlyByteBuf::writeComponent);
    }

    @Override
    public boolean isSkippable() {
        return true;
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleChatPreview(this);
    }
}
