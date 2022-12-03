package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundCustomChatCompletionsPacket(ClientboundCustomChatCompletionsPacket.Action action, List<String> entries) implements Packet<ClientGamePacketListener> {
    public ClientboundCustomChatCompletionsPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(ClientboundCustomChatCompletionsPacket.Action.class), buf.readList(FriendlyByteBuf::readUtf));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeEnum(this.action);
        buf.writeCollection(this.entries, FriendlyByteBuf::writeUtf);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handleCustomChatCompletions(this);
    }

    public static enum Action {
        ADD,
        REMOVE,
        SET;
    }
}
