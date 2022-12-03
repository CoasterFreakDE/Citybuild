package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.StringUtil;

public record ServerboundChatCommandPacket(String command, Instant timeStamp, long salt, ArgumentSignatures argumentSignatures, boolean signedPreview, LastSeenMessages.Update lastSeenMessages) implements Packet<ServerGamePacketListener> {
    public ServerboundChatCommandPacket {
        command = StringUtil.trimChatMessage(command);
    }

    public ServerboundChatCommandPacket(FriendlyByteBuf buf) {
        this(buf.readUtf(256), buf.readInstant(), buf.readLong(), new ArgumentSignatures(buf), buf.readBoolean(), new LastSeenMessages.Update(buf));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.command, 256);
        buf.writeInstant(this.timeStamp);
        buf.writeLong(this.salt);
        this.argumentSignatures.write(buf);
        buf.writeBoolean(this.signedPreview);
        this.lastSeenMessages.write(buf);
    }

    @Override
    public void handle(ServerGamePacketListener listener) {
        listener.handleChatCommand(this);
    }
}
