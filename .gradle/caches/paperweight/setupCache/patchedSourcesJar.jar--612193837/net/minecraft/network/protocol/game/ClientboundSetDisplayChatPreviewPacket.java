package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundSetDisplayChatPreviewPacket(boolean enabled) implements Packet<ClientGamePacketListener> {
    public ClientboundSetDisplayChatPreviewPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.enabled);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleSetDisplayChatPreview(this);
    }
}
