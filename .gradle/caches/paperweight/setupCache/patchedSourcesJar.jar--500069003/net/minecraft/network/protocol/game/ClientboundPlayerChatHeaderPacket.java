package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageHeader;
import net.minecraft.network.protocol.Packet;

public record ClientboundPlayerChatHeaderPacket(SignedMessageHeader header, MessageSignature headerSignature, byte[] bodyDigest) implements Packet<ClientGamePacketListener> {
    public ClientboundPlayerChatHeaderPacket(PlayerChatMessage message) {
        this(message.signedHeader(), message.headerSignature(), message.signedBody().hash().asBytes());
    }

    public ClientboundPlayerChatHeaderPacket(FriendlyByteBuf buf) {
        this(new SignedMessageHeader(buf), new MessageSignature(buf), buf.readByteArray());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        this.header.write(buf);
        this.headerSignature.write(buf);
        buf.writeByteArray(this.bodyDigest);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePlayerChatHeader(this);
    }
}
