package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;

public record ClientboundPlayerChatPacket(PlayerChatMessage message, ChatType.BoundNetwork chatType) implements Packet<ClientGamePacketListener> {
    public ClientboundPlayerChatPacket(FriendlyByteBuf buf) {
        this(new PlayerChatMessage(buf), new ChatType.BoundNetwork(buf));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        this.message.write(buf);
        this.chatType.write(buf);
    }

    @Override
    public void handle(ClientGamePacketListener listener) {
        listener.handlePlayerChat(this);
    }

    @Override
    public boolean isSkippable() {
        return true;
    }

    public Optional<ChatType.Bound> resolveChatType(RegistryAccess dynamicRegistryManager) {
        return this.chatType.resolve(dynamicRegistryManager);
    }
}
