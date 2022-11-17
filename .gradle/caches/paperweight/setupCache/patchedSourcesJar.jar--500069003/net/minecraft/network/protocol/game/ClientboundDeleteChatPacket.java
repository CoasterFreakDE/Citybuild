package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;

public record ClientboundDeleteChatPacket(MessageSignature messageSignature) implements Packet<ClientGamePacketListener> {
    public ClientboundDeleteChatPacket(FriendlyByteBuf buf) {
        this(new MessageSignature(buf));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        this.messageSignature.write(buf);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleDeleteChat(this);
    }
}
