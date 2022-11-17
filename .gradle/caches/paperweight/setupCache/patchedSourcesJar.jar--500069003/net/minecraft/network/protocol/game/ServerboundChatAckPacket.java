package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.Packet;

public record ServerboundChatAckPacket(LastSeenMessages.Update lastSeenMessages) implements Packet<ServerGamePacketListener> {
    public ServerboundChatAckPacket(FriendlyByteBuf buf) {
        this(new LastSeenMessages.Update(buf));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        this.lastSeenMessages.write(buf);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleChatAck(this);
    }
}
