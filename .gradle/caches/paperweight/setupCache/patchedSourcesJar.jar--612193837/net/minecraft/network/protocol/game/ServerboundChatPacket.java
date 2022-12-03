package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSigner;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

public record ServerboundChatPacket(String message, Instant timeStamp, long salt, MessageSignature signature, boolean signedPreview, LastSeenMessages.Update lastSeenMessages) implements Packet<ServerGamePacketListener> {
    public ServerboundChatPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256), buf.readInstant(), buf.readLong(), new MessageSignature(buf), buf.readBoolean(), new LastSeenMessages.Update(buf));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.message, 256);
        buf.writeInstant(this.timeStamp);
        buf.writeLong(this.salt);
        this.signature.write(buf);
        buf.writeBoolean(this.signedPreview);
        this.lastSeenMessages.write(buf);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleChat(this);
    }

    public MessageSigner getSigner(ServerPlayer sender) {
        return new MessageSigner(sender.getUUID(), this.timeStamp, this.salt);
    }
}
